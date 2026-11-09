# 腾讯云 COS 与短信接入设计

[返回总览](../README.md) · [用户体系接入](EXTENDING.md) · [配置](CONFIGURATION.md)

状态：**对接文档和设计，尚未实现应用端点，未调用 COS 上传或发送真实短信**。此章不能用作云业务验收通过证明。参考资料核对于 2026-11-09。

## 1. 凭据与配置准备

腾讯云 SecretId/SecretKey 与中间件用户名密码不同。前端不能获得永久云密钥。

建议的未来环境变量：

| 变量 | 用途 |
|---|---|
| TENCENT_SECRET_ID / TENCENT_SECRET_KEY | 服务端 CAM 身份 |
| COS_REGION | 存储桶实际地域 |
| COS_BUCKET | 完整 BucketName-APPID |
| SMS_REGION | 短信 API 支持的地域 |
| SMS_SDK_APP_ID | 短信应用 ID |
| SMS_SIGN_NAME | 已审核签名内容 |
| SMS_TEMPLATE_ID | 已审核模板 ID |

**这些变量目前尚未被示例读取**。添加到 .env 不会自动产生云功能，需要应用配置类和客户端适配层。

Maven 坐标：

- COS：com.qcloud:cos_api
- SMS：com.tencentcloudapi:tencentcloud-sdk-java-sms
- STS：com.tencentcloudapi:tencentcloud-sdk-java-sts

2026-11-07 的历史版本核验分别为 5.6.283、3.2.18、3.2.6，尚未加入 POM。正式实施时重新核对版本、依赖冲突和 JDK 27 行为，固定通过测试的版本，不使用 LATEST。

## 2. COS 准备步骤

1. 在 COS 控制台选择或创建桶，记录完整桶名和地域。
2. 明确私有文件与公开素材的访问策略。
3. 给后端 CAM 身份授予需要的对象路径与操作权限。
4. 浏览器直传时配置对应 Origin、Method 和 Header 的 CORS。
5. 定义对象 Key 规则、大小/类型限制与业务归属。
6. 在独立测试前缀验证上传、读取、删除和无权限访问。

Java SDK 的主要客户端为 COSClient，提供对象写入、读取、删除和预签名等能力；客户端配置和凭据按官方 SDK 创建。[COS Java 入门](https://cloud.tencent.com/document/product/436/10199)

## 3. 推荐的文件业务流程

以下接口名称是设计方案，不是当前可调用端点。

~~~text
已认证用户申请上传
  → 后端检查用户、用途、文件类型/大小
  → 生成业务文件记录和唯一 objectKey
  → 返回限定用途的上传授权
  → 客户端上传 COS
  → 后端核验对象存在及元数据
  → 文件记录由 pending 改为 ready
  → 业务对象关联 fileId
~~~

Key 设计建议：
`tenant/<tenantId>/users/<userId>/<yyyyMM>/<随机ID>.<后缀>`。

不要使用客户端任意提交的完整路径，也不要用原文件名作为全局唯一 Key。数据库建议记录：

~~~json
{
  "_id":"fileId",
  "ownerId":"userId",
  "bucket":"BucketName-APPID",
  "region":"实际地域",
  "objectKey":"服务端生成的Key",
  "originalName":"原始文件名",
  "contentType":"image/png",
  "size":12345,
  "status":"pending",
  "createdAt":"时间"
}
~~~

下载前校验业务权限，生成短期授权。数据库保存 objectKey 而非即将过期的签名 URL。删除采用状态记录和补偿任务，避免数据库删了但对象残留、或对象删了但业务仍引用。

## 4. STS 与预签名的选择

| 方式 | 适合情况 | 需要控制 |
|---|---|---|
| 服务端代理上传 | 文件较小、后端需处理内容 | 请求体大小、流量、超时 |
| 单对象预签名 URL | 指定 Key 的短时直接上传/下载 | Key、HTTP 方法、有效期 |
| STS 临时凭据 | 客户端需要 SDK 多步骤操作 | resource/action 范围、期限 |

STS 返回临时 ID、临时 Key 和 Token，权限必须限定到业务需要的路径和操作。[STS 指引](https://cloud.tencent.com/document/product/436/14048)

预签名 URL 的持有者在有效期内可以执行已授权操作，不应写入公开日志。大文件和分块上传按 SDK 能力设计，不能把简单上传示例当成所有文件场景。[预签名上传](https://cloud.tencent.com/document/product/436/14114)

## 5. COS 的失败与补偿

| 情况 | 处理建议 |
|---|---|
| 申请授权后没有上传 | pending 超时清理 |
| 上传成功但确认请求丢失 | 通过 fileId 查询并幂等确认 |
| 同 Key 重复上传 | 明确是否覆盖；默认采用唯一 Key |
| 下载签名过期 | 重新鉴权并签发 |
| 删除对象失败 | 记录待清理任务并有限重试 |
| 内容类型伪装 | 服务端校验内容，不只信扩展名 |
| 业务记录存在、对象不存在 | 告警和补偿，不返回虚假成功 |

文件生命周期与 MongoDB 事务不能直接形成跨系统原子提交，需要状态机和幂等补偿。

## 6. 短信准备步骤

1. 开通短信产品，创建应用，记录 SmsSdkAppId。
2. 申请签名和正文模板并等待审核。
3. 明确模板变量数量、顺序和用途。
4. 准备具有短信调用权限的 CAM 身份。
5. 国内短信确认套餐/余额以及目标号码范围。
6. 验收使用明确授权的接收号码，记录发送结果和回执。

官方 Java SDK 包含发送、回执和统计接口，建议交给 SDK 完成云 API 签名。[短信 Java SDK](https://cloud.tencent.com/document/product/382/43194)

## 7. SendSms 参数

接口域名 `sms.tencentcloudapi.com`，Action 为 `SendSms`，API Version 为 `2021-01-11`，Region 使用产品支持值。

| 参数 | 业务含义 |
|---|---|
| PhoneNumberSet | 接收号码数组，建议 E.164 |
| SmsSdkAppId | 短信应用 ID |
| TemplateId | 已审核模板 ID |
| SignName | 签名内容；国内短信必填，不是签名 ID |
| TemplateParamSet | 按模板顺序填变量 |
| SessionContext | 可选业务关联信息 |

单次最多 200 个号码，境内/境外不能混发。逐项检查 SendStatusSet，记录 RequestId 和可用的发送标识；接口受理与最终送达不同。[SendSms API](https://cloud.tencent.com/document/product/382/55981)

业务请求结构示意，无法直接作为完整的签名 HTTP 请求执行：

~~~json
{
  "PhoneNumberSet":["替换为已授权的E.164号码"],
  "SmsSdkAppId":"实际应用ID",
  "SignName":"已审核签名内容",
  "TemplateId":"已审核模板ID",
  "TemplateParamSet":["动态验证码","有效分钟数"]
}
~~~

## 8. 验证码业务设计

这是应用层设计，不是腾讯云接口自动实现的能力：

1. 按手机号、IP、用户/设备及业务场景做发送频控。
2. 使用安全随机数生成验证码。
3. Redis 保存验证码摘要、场景、有效期和尝试次数；不在日志打印验证码。
4. 发送结果为失败时按错误类型更新状态，避免直接触发无上限重试。
5. 验证时原子检查、计数和消费，成功后一次性失效。
6. 登录验证码和改密码验证码使用不同场景 key。
7. 超时但结果不明时，避免立即重新发送产生重复收费和骚扰。
8. 查询送达回执，用业务请求号关联；回执不能当作用户已完成身份验证。

具体有效期和重发间隔由产品确定并配置，不写死在控制器里。

## 9. Reactor / 虚拟线程接法

COS / SMS 同步 SDK 调用需要在框架适配层隔离，保留 Mono / Flux 接口：

~~~java
return Mono.fromCallable(() -> smsClient.SendSms(request))
    .subscribeOn(Schedulers.boundedElastic());
~~~

示意依赖已有 smsClient 和 request；当前代码未添加该客户端。开启经过验证的虚拟线程配置后，boundedElastic 可以使用虚拟线程。SDK 自身连接超时、请求超时和连接池仍需要设置。

超时取消不保证云端没有执行，因此不能给所有发送操作无条件 retry。客户端按 SDK 约定复用和关闭，不为每次请求创建一整套连接池。

## 10. 网络与端口

调用 COS / SMS 通常需要应用访问外部 HTTPS 443 和正常 DNS，不需要为发短信额外开放数据库入站端口。若设计云回调入口，再单独部署 HTTPS、认证和幂等处理。

## 11. 接入完成的验收标准

COS：上传成功、对象校验、私有下载、过期授权拒绝、跨用户拒绝、大小/类型限制、重复确认、删除及孤儿清理。

SMS：正确模板受理、错误参数拒绝、逐号码状态处理、回执关联、频控、验证码过期/错误次数/一次性使用、超时结果不明处理。

验收记录需写实际环境、版本、请求号和清理结果。当前没有这些实机云调用记录，不能将本章示例列为通过。

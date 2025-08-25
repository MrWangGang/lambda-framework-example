package ace.microservices.dict.server;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.*;



public class Learning {

        public static void main(String[] args) throws InterruptedException {
            //fluxTest1();
            //fluxTest2();
            //fluxTest3();
            //fluxTest4();
            //testCombineLatest();
            /*Mono<Integer> a = Mono.defer(()->{
                System.out.println("---->1");
                return Mono.empty();
            });
            a.switchIfEmpty(Mono.defer(()->{
                System.out.println("---->3");
                return Mono.just(1);
            })).flatMap(e->{
                System.out.println("---->2");
                return Mono.just(e);
            }).subscribe();

            //用以下的方式来对空判断后，不再执行a.flatmap
            //因为是先执行a.flatmap，如果e为空则a.flatmap不执行，转而执行.switch
            //如果e不为空则执行a.flatmap，swit不执行
            a.flatMap(e->{
                System.out.println("---->2");
                return Mono.just(e);
            }).switchIfEmpty(Mono.defer(()->{
                System.out.println("---->3");
                return Mono.just(1);
            })).subscribe();*/
            get().map(str->{
                if(str == null){
                    return false;
                }
                return true;
            }).switchIfEmpty(Mono.just(false)).subscribe(e->{
                System.out.println(e);
            });

    }
    public static Mono<String> get(){
            return Mono.just("1");
    }


    public static void testGroup(){
        Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
                .groupBy(i -> i % 2 == 0 ? "even" : "odd")
                .subscribe(f -> f.subscribe(t->System.out.println(f.key() + " : " + t)));
    }

    public static void testJoin(){
        Flux<Integer> f1 = Flux.just(1,2,3,10,11,12,13,14);
        Flux<Integer> f2 = Flux.just(10,12,13,14,15,16);
        f1.join(f2,x->Flux.never(),y-> Flux.never(),(x,y)->{
            return x+","+y;
        }).subscribe(System.out::println);
    }
    public static void testGroupJoin(){

        Flux<Integer> f1 = Flux.just(1,2,3,10,11,12,13,14);
        Flux<Integer> f2 = Flux.just(10,12,13,14,15,16);
        f1.groupJoin(f2,x->Flux.never(),y-> Flux.never(),(x,y)->{
            return y.flatMap(e->{
                return Mono.just(x+","+e);
            });
        }).flatMap(e->e).subscribe(System.out::println);
        //.map 无法得到结果
        //groupJoin返回的是一个fluxFlatMap(flatmap)  流
        // map 只能映射元素 无法映射 流
    }

    public static void testCombineLatest(){
        Flux<Integer> f1 = Flux.just(14);
        Flux<Integer> f2 = Flux.just(10,12,13,14,15,16);
        Flux.combineLatest(f1,f2,(x,y)->{
            return x+","+y;
        }).subscribe(System.out::println);
    }


    //flatmap是异步转换 订阅者不须要等待元素发射,发射一个元素就会通知订阅者，订阅者消费时间比发布者发布时间长，也不会影响发布者发布下一个元素
    public static void fluxTest3() throws InterruptedException {
        Flux.just(1,2,3,4)
                .flatMap(e -> {
                    System.out.println("转换"+e+ " "+Thread.currentThread().getName());
                    return Flux.just(e*2);
                })
                .publishOn(Schedulers.boundedElastic())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(e->{System.out.println("get:"+e + " "+Thread.currentThread().getName());});
        Thread.sleep(10000);
    }


    //map是同步转换 订阅者必须等待元素发射
    public static void fluxTest4() throws InterruptedException {

        Flux.just(1,2,3,4)
                .flatMap(e -> {
                    System.out.println("转换" + e + " " + Thread.currentThread().getName());
                    return Mono.just(e);
                })
                .publishOn(Schedulers.boundedElastic())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(e -> {
                    System.out.println("get:" + e + " " + Thread.currentThread().getName());
                });
        System.out.println("hh");

        Thread.sleep(100000);
    }



    //回调地狱
    public static void fluxTest1() throws InterruptedException {
        //flatmap1.flatmap2.subscribe
        //subscribe 订阅的flatmap1
        //flatmap2处理flatmap1中所有的元素
        //这是嵌套式的调用
        Flux.just(1,2,3,4,5,6)
                .flatMap(u -> {
                    System.out.println("省 :"+u + " "+Thread.currentThread().getName());
                    return Mono.just(u+"@");
                }).publishOn(Schedulers.boundedElastic())//在这publishOn 会让flatmap操作所有元素被线程控制
                .flatMap(u -> {
                    System.out.println("市 :"+u + " "+Thread.currentThread().getName());
                    //在这publishOn 会让flatmap里的每个元素操作被线程控制.publishOn(Schedulers.boundedElastic());
                    return Mono.just(u+"&");
                    //return Mono.just(u+"&");
                })//在这publishOn 会让subscribeOn被其他线程控制
                //影响第一个flatmap 因为第二个被 publishOn覆盖了
                // ，subscribeOn 是影响全局的 碰到publishOn 就停止了范围
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(e->{
                    System.out.println("subscribe: "+e + " " + Thread.currentThread().getName());
                });
        System.out.println("main to hi------------------------");
        Thread.sleep(100000);
    }


    //主线程不影响，异步任务操作
    public static void fluxTest2() throws InterruptedException {
        Flux user = Flux.just(1,2);
        user.flatMap(u -> {
                    System.out.println(String.format(
                            "Saving person from thread %s", Thread.currentThread().getName()));
                    return Mono.just(u);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        user.flatMap(u -> {
                    System.out.println(String.format(
                            "update person from thread %s", Thread.currentThread().getName()));
                    return Mono.just(u);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        System.out.println("main to hi------------------------");
        Thread.sleep(10000);
    }

    public static Scheduler MyScheduler() {
        Executor executor = new ThreadPoolExecutor(
                10,  //corePoolSize
                10,  //maximumPoolSize
                0L, TimeUnit.MILLISECONDS, //keepAliveTime, unit
                new LinkedBlockingQueue<>(1000),  //workQueue
                Executors.defaultThreadFactory()
        );
        return Schedulers.fromExecutor(executor);
    }
}

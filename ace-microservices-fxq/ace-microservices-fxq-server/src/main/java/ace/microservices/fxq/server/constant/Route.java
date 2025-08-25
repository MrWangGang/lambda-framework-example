package ace.microservices.fxq.server.constant;

public class Route {
    //一级路由
    protected static final String ROUTE = "/ace/microservices/fxq";
    //二级路由
    protected static final String EMPLOYEE = ROUTE+"/employee";
    //二级路由
    protected static final String CUST = ROUTE+"/cust";
    //三级路由
    public static final String EMPLOYEE_LOGIN = EMPLOYEE + "/login";
    public static final String EMPLOYEE_REGISTER = EMPLOYEE + "/register";
    //三级路由
    public static final String CUST_PUT = CUST + "/put";
    public static final String CUST_FIND = CUST + "/find";
    public static final String CUST_GET = CUST + "/get";
    public static final String CUST_PAGEDETAILS = CUST + "/pageDetails";
    public static final String CUST_PAGE = CUST + "/page";
    public static final String TEST_MESSAGE = CUST + "/test";
    public static final String PUSH_KAFKA = CUST + "/pushKafka";
    public static final String PUSH_ZK = CUST + "/pushZk";
    public static final String TEST_ZK = CUST + "/testZk";




}

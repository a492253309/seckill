package com.shaohao.seckill.utils;


import okhttp3.Call;
import okhttp3.Request;

import java.io.IOException;

/**
 * HTTP客户端常用接口封装，简化操作，提升性能，后续支持注解
 * 参考RestTemplate
 * @version
 *
 */
public interface HTTP {

    String ReqExecute(Request request) throws IOException;
    Call ReqExecuteCall(Request request);

    /**
     *
     * GET同步方法
     *
     * @author liuyi 2016年7月17日
     * @param url
     * @return
     * @throws HttpException
     * @throws IOException
     */
    String GET(String url, String token) throws HttpException,IOException;

    /**
     * post json格式请求
     * @param baseUrl
     * @param jsonBody
     * @return
     * @throws HttpException
     * @throws IOException
     */
    String postJson(String baseUrl, String jsonBody, String token)throws IOException;

}


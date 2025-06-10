package com.shaohao.seckill.utils;



import okhttp3.Response;

import java.io.IOException;

/**
 * http自定义异常类
 *
 * @version
 * @author shaohao  2023年6月17日 下午2:28:00
 *
 */
public class HttpException extends IOException {
    private Response response;
    private static final long serialVersionUID = -4802242016364002941L;

    public HttpException(IOException e) {
        super(e);
    }

    public HttpException(Response response, String message) {
        super(message);
        this.setResponse(response);
    }

    /**
     * @return the response
     */
    public Response getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(Response response) {
        this.response = response;
    }
}

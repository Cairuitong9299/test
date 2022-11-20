package com.example.test.utils;

import com.alibaba.fastjson.JSON;
import com.bdp.idmapping.response.Response;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @Auther: CAI
 * @Date: 2022/11/10 - 11 - 10 - 0:03
 * @Description: com.example.test.utils
 * @version: 1.0
 */
public class ResponseUtils {

    public  static  void  write(ServletResponse response, Response result){
        PrintWriter out=null;
        try {
            out=response.getWriter();
            out.write(JSON.toJSONString(result));
            out.flush();
        }catch (IOException e){

        }finally {
             out.close();
        }
    }
}

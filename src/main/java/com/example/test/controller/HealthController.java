package com.example.test.controller;

import org.springframework.web.bind.annotation.*;

/**
 * @Auther: CAI
 * @Date: 2022/6/25 - 06 - 25 - 15:46
 * @Description: com.example.test.controller
 * @version: 1.0
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @RequestMapping(value = "check" , method = RequestMethod.GET)
    public String check(@RequestParam(value="sq") String st, int i){
        String sl;
        sl = st + i;
        return sl;
    }

    @RequestMapping(value = "check1" , method = RequestMethod.POST)
    public String check1(@RequestBody String name,@RequestParam String birth){
        String namebirth = "姓名：" + name + "\t出生日期" + birth;
        return namebirth;
    }


}

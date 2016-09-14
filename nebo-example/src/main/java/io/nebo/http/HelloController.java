package io.nebo.http;


import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by pengbo on 2016/5/4.
 */

@Controller
@RequestMapping(value = "/hello")
public class HelloController {

    @RequestMapping(value = "/user")
    @ResponseBody
    public String user(HttpServletRequest request, HttpServletResponse response) {
        return "你好" + request.getParameter("name");
    }


}

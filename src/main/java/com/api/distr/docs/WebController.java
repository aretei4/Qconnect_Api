package com.api.distr.docs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {
   
    public String redirect() {
        return "forward:/index.html";
    }
}


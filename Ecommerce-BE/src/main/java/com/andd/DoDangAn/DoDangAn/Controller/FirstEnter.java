package com.andd.DoDangAn.DoDangAn.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class FirstEnter {
    @RequestMapping(value="/", method = RequestMethod.GET)
    public String firstenter(){
        return "redirect:/api/user/home";
    }
}
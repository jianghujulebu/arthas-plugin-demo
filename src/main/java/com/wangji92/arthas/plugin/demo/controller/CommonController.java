package com.wangji92.arthas.plugin.demo.controller;

import com.wangji92.arthas.plugin.demo.service.ArthasTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Random;

/**
 * arthas 体验demo
 *
 * @author 汪小哥
 * @date 28-03-2020
 */
@Controller
@RequestMapping("/")
public class CommonController {

    @Autowired
    private ArthasTestService arthasTestService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * tt 测试 or 通过ognl 调用spring context getBean 调用处理
     * ognl -x 3 '#springContext=@com.wangji92.arthas.plugin.demo.common.ApplicationContextProvider@context,#springContext.getBean("commonController").getRandomInteger()' -c e374b99
     *
     * 记录时间隧道，重新触发一次
     * tt -t com.wangji92.arthas.plugin.demo.controller.CommonController getRandomInteger -n 5
     * tt -p -i 1000
     *
     * @return
     */
    @RequestMapping("/getRandomInteger")
    @ResponseBody
    public Integer getRandomInteger() {
        return new Random().nextInt(1000);

    }

    /**
     * use trace -E
     * trace -E com.wangji92.arthas.plugin.demo.controller.CommonController|com.wangji92.arthas.plugin.demo.service.ArthasTestService traceE|doTraceE -n 5
     *
     * ##包含jdk的调用
     *trace -E com.wangji92.arthas.plugin.demo.controller.CommonController|com.wangji92.arthas.plugin.demo.service.ArthasTestService traceE|doTraceE -n 5 --skipJDKMethod false
     * @param name
     * @return
     */
    @RequestMapping("/trace/{name}")
    @ResponseBody
    public String traceE(@PathVariable String name) {
        if (StringUtils.isEmpty(name)) {
            name = "汪小哥";
        } else {
            name = name + "汪小哥";
        }
        return arthasTestService.doTraceE(name);
    }

    /**
     * 排查异常场景
     * trace com.wangji92.arthas.plugin.demo.controller.CommonController traceException -n 5
     * watch com.wangji92.arthas.plugin.demo.controller.CommonController traceException '{params,returnObj,throwExp}' -n 5 -x 3
     *
     * @return
     */
    @RequestMapping("traceException")
    @ResponseBody
    public String traceException() {
        arthasTestService.traceException(1);
        return "ok";
    }

    /**
     * 环境变量优先级问题排查
     * 目前只支持获取静态static spring context
     * 获取所有的环境变量 按照优先级排序
     * ognl -x 3 '#springContext=@com.wangji92.arthas.plugin.demo.common.ApplicationContextProvider@context,#allProperties={},#standardServletEnvironment=#propertySourceIterator=#springContext.getEnvironment(),#propertySourceIterator=#standardServletEnvironment.getPropertySources().iterator(),#propertySourceIterator.{#key=#this.getName(),#allProperties.add("                "),#allProperties.add("------------------------- name:"+#key),#this.getSource() instanceof java.util.Map ?#this.getSource().entrySet().iterator.{#key=#this.key,#allProperties.add(#key+"="+#standardServletEnvironment.getProperty(#key))}:#{}},#allProperties' -c e374b99
     *
     * 选中 custom.name 获取当前的变量的信息
     * ognl -x 3 '#springContext=@com.wangji92.arthas.plugin.demo.common.ApplicationContextProvider@context,#springContext.getEnvironment().getProperty("custom.name")' -c e374b99
     * @return
     */
    @RequestMapping("environmentPriority")
    @ResponseBody
    public String environmentPriority() {
        return applicationContext.getEnvironment().getProperty("custom.name");
    }

    /**
     * 复杂参数调用 场景
     * static spring context
     * ognl -x 3 '#user=new com.wangji92.arthas.plugin.demo.controller.User(),#user.setName("wangji"),#user.setAge(27L),#springContext=@com.wangji92.arthas.plugin.demo.common.ApplicationContextProvider@context,#springContext.getBean("commonController").complexParameterCall(#{"wangji":#user})' -c e374b99
     *
     * watch get spring context 备注 需要调用一次方法
     * watch -x 3 -n 1  org.springframework.web.servlet.DispatcherServlet doDispatch '#user=new com.wangji92.arthas.plugin.demo.controller.User(),#user.setName("wangji"),#user.setAge(27L),@org.springframework.web.context.support.WebApplicationContextUtils@getWebApplicationContext(params[0].getServletContext()).getBean("commonController").complexParameterCall(#{"wangji":#user})'
     *
     * tt get spring context ，only first get time index ok
     * tt -w '#user=new com.wangji92.arthas.plugin.demo.controller.User(),#user.setName("wangji"),#user.setAge(27L),target.getApplicationContext().getBean("commonController").complexParameterCall(#{"wangji":#user})' -x 3 -i 1000
     * @return
     */
    @RequestMapping("complexParameterCall")
    @ResponseBody
    public String complexParameterCall(@RequestBody  Map<String, User> names) {
        if (names == null) {
            return "EMPTY";
        }
        return names.toString();
    }

}

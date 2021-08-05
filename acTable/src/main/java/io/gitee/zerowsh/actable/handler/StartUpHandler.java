//package io.gitee.zerowsh.actable.handler;
//
//import io.gitee.zerowsh.actable.service.BaseDatabaseImpl;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//
///**
// * 启动时进行处理的实现类
// *
// * @author zero
// */
//@Component
//@Slf4j
//public class StartUpHandler implements InitializingBean {
//    @Resource
//    private BaseDatabaseImpl baseDatabase;
//
//    @PostConstruct
//    public void startHandler() {
//
//        System.out.println("---============--");
//    }
//
//    static {
//        System.out.println("---static--");
//    }
//
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        System.out.println("=============");
//    }
//}

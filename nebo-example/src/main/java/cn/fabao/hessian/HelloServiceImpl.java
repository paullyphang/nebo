package cn.fabao.hessian;

import cn.fabao.hessian.HessianEndpoint;
import org.springframework.stereotype.Component;

/**
 * Created by pengbo on 2016/7/19.
 */
@Component
@HessianEndpoint(servicePattern = "/helloService")
public class HelloServiceImpl implements  IHelloService {

    @Override
    public String sayHi(String user) {
        System.out.println("user >>>>>>>>>>>>>>>>>>>>>> " + user);
        return "sayHi ： " + user;

    }
}

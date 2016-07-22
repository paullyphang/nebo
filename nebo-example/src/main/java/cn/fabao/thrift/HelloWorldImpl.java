package cn.fabao.thrift;

import cn.fabao.thrift.ThriftEndpoint;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

/**
 * Created by pengbo on 2016/6/17.
 */
@Component
@ThriftEndpoint(serviceName="helloWorld")
public class HelloWorldImpl implements HelloWorld.Iface {

    @Override
    public Result createNewBaseResInfo(User user) throws TException {
        System.out.println(user.name + ">>" + user.id + ">>" + user.isIsman());
        Result result = new Result();
        result.setMsg(user.name + ">>" + user.id + ">>" + user.isIsman() + "你好");
        return result;
    }
}

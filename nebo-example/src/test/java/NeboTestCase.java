import io.nebo.App;
import io.nebo.hessian.IHelloService;
import io.nebo.thrift.HelloWorld;
import io.nebo.thrift.Result;
import io.nebo.thrift.User;
import com.caucho.hessian.client.HessianProxyFactory;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import java.net.MalformedURLException;

/**
 * Created by pengbo on 2016/7/22.
 */
public class NeboTestCase {

    @Before
    public void start(){
        SpringApplication.run(App.class);
    }

    @Test
    public void hessianTest() throws MalformedURLException {
        String url = "http://localhost:8080/hessian/helloService";
        HessianProxyFactory factory = new HessianProxyFactory();
        IHelloService basic = (IHelloService) factory.create(IHelloService.class, url);
        System.out.println("hello(): " + basic.sayHi("233333333333333333333333333333333始发地"));

    }

    @Test
    public void thriftTest() throws TException {
        TSocket transport = new TSocket("127.0.0.1", 8080);
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,"helloWorld");
        HelloWorld.Client client = new HelloWorld.Client(mp1);
        User user = new User();
        user.setName("{\"fabaoId\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}");
        user.setId(234242453);
        user.setIsman(true);
        Result result = client.createNewBaseResInfo(user);
        System.out.println(result.getMsg());
        System.out.println("end>>>>>>>>>>>>>>>>");

    }

}

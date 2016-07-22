
namespace java com.fabao.service.thrift



struct User{
	1:i64 id,
	2:string name,
	3:bool isman
}

struct Result{
    1:string msg
}

service HelloWorld {
        Result createNewBaseResInfo(1:User user);
}
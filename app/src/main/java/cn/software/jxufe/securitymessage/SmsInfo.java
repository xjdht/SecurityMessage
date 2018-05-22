package cn.software.jxufe.securitymessage;

/**
 * Created by lenovo on 2018/5/6.
 */

public class SmsInfo {
    private String read;
    private String type;   //1是接收，2是发出
    private String name;
    private String number;
    private String body;
    private String date;
    private String id;
    private String threadId;
//    public SmsInfo(String type, String name, String number, String body, String date, String id) {
//        setType(type);
//        setName(name);
//        setNumber(number);
//        setBody(body);
//        setDate(date);
//        setId(id);
//    }

    public SmsInfo(String type, String name, String number, String body, String date, String id, String threadId) {
        setType(type);
        setName(name);
        setNumber(number);
        setBody(body);
        setDate(date);
        setId(id);
        setThreadId(threadId);
    }
    public SmsInfo(String type, String name, String number, String body, String date) {
        setType(type);
        setName(name);
        setNumber(number);
        setBody(body);
        setDate(date);
    }

    public SmsInfo(String type, String number, String name, String body, String date, String id) {
        setType(type);
        setNumber(number);
        setName(name);
        setBody(body);
        setDate(date);
        setId(id);
    }

    public SmsInfo(String id, String name, String body, String date) {
        setName(name);
        setBody(body);
        setDate(date);
        setId(id);
    }

   /* public SmsInfo(String type, String name, String number, String body, String date) {
        setType(type);
        setName(name);
        setNumber(number);
        setBody(body);
        setDate(date);
    }*/

    public SmsInfo( String name,String body, String date) {
        setName(name);
        setBody(body);
        setDate(date);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getId() {return id;}

    public void setId(String id) {this.id = id;}

    public void setThreadId(String threadId) {this.threadId = threadId;}

    public void setRead(String read) {this.read = read;}

    public String getRead() {return read;}
    public String getThreadId(String threadId) {return threadId;}
    @Override
    public String toString() {
        return "SmsInfo{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", body='" + body + '\'' +
                ", date='" + date + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
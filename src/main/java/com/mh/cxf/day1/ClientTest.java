package com.mh.cxf.day1;

public class ClientTest {
	
	public static void main(String[] args) throws Exception {
		HelloWorld helloWorld = new HelloWorldService().getHelloWorldPort();
		String result = helloWorld.sayHi("jason");
		System.out.println(result);
	}

}

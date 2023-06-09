package com.cos.reflect.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cos.reflect.anno.RequestMapping;
import com.cos.reflect.controller.UserController;

// 목적 : 요청에 따라 분기
public class Dispatcher implements Filter {
	
	private boolean isMatching = false;
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		System.out.println("디스패쳐 진입");
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		
		//System.out.println("컨텍스트패스 : " + request.getContextPath());  // 프로젝트 시작 주소
		//System.out.println("식별자주소 :" + request.getRequestURI());  // 끝주소
		//System.out.println("전체주소 : "+ request.getRequestURL());  // 전체주소
		
		// /user 파싱하기
		String endPoint = request.getRequestURI().replaceAll(request.getContextPath(),"");
		System.out.println("엔드포인트 : " + endPoint);
		
		UserController userController = new UserController();
//		if (endPoint.equals("/join")) {
//			userController.join();
//		} else if (endPoint.equals("/login")) {
//			userController.login();
//		} else if (endPoint.equals("/user")) {
//			userController.user();
//		}
		
		// 리플렉션 -> 메서드를 런타임 시점에서 메서드를 찾아내서 실행
		Method[] methods = userController.getClass().getDeclaredMethods(); // 그 파일의 메서드만!!
//		for (Method method : methods) {
//			//System.out.println(method.getName());
//			if (endPoint.equals("/" +method.getName())){
//				try {
//					method.invoke(userController);
//				} catch (Exception e) {
//					e.printStackTrace();
//				} 
//			}
//		}
		//System.out.println();
		
		for (Method method : methods) { // 4바퀴 (join,login, user, hello)
			Annotation annotation = method.getDeclaredAnnotation(RequestMapping.class);
			//Annotation[] annotations = method.getDeclaredAnnotations();
			//System.out.println(annotation);
			RequestMapping requestMapping = (RequestMapping) annotation;
			System.out.println(requestMapping.value());
			
			if( requestMapping.value().equals(endPoint)) {
				isMatching = true;
				try {
					Parameter[] params = method.getParameters();
					String path = null;
					if (params.length != 0) {
						//System.out.println("params[0].getType() : " + params[0].getType());
						// 해당 dtoInstance를 리플렉션해서 set함수 호출(username, password)
						Object dtoInstance = params[0].getType().getDeclaredConstructor().newInstance();
						setData(dtoInstance,request);
						path = (String) method.invoke(userController,dtoInstance);
					} else {
						path = (String) method.invoke(userController);
					}
					
					// sendRedirect, requestDispatcher
					
					RequestDispatcher dis = request.getRequestDispatcher(path); // 필터를 다시 안탐!
					dis.forward(request, response);
					//response.sendRedirect(path);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}
		if (isMatching == false) {
		response.setContentType("text/htm; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.println("잘못된 주소 요청입니다. 404 에러");
		}
	}
	
	private <T> void setData(T instance, HttpServletRequest request) {
		Enumeration<String> keys = request.getParameterNames(); // 크기 : 2 (username, password)
		// keys 값을 변형 username => setUsername 
		// keys 값을 번형 password => setPassword
		while(keys.hasMoreElements()) {  // 2번 돈다.
			String key = (String) keys.nextElement();
			String methodKey = keyToMethodKey(key);  // setUsername
			
			Method[] methods = instance.getClass().getDeclaredMethods();
			
			for (Method method : methods) {
				if(method.getName().equals(methodKey)) {
					try {
						System.out.println("method.getName() : " + method.getName());
						System.out.println("method.getParameterTypes()[0] : "+ method.getParameterTypes()[0].getName());
						System.out.println("method.getTypeParameters(): " +Arrays.toString(method.getTypeParameters()));
						System.out.println("method.getParameters()[0].getName() : " +method.getParameters()[0].getName());
						System.out.println();

						if (method.getParameterTypes()[0].getName().equals("int")) {
							method.invoke(instance, Integer.parseInt(request.getParameter(key)) );
						} else {
							method.invoke(instance, request.getParameter(key) );
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	private String keyToMethodKey(String key) {
		String firstKey = "set";
		String upperKey = key.substring(0,1).toUpperCase();
		String remainKey = key.substring(1);
		return firstKey + upperKey + remainKey;
	}
}



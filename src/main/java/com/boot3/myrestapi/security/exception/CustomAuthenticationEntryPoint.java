package com.boot3.myrestapi.security.exception;

import com.boot3.myrestapi.commons.exception.ErrorObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/*
    401 인증 실패를 처리하는 클래스
 */
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        response.setContentType("application/json;charset=UTF-8");
        //Set response status code 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ObjectMapper mapper = new ObjectMapper();
        // Create response content
        ErrorObject errorObject = new ErrorObject();
        errorObject.setStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
        errorObject.setMessage("요청된 리소스에 대한 유효한 인증 자격 증명이 실패 하였습니다!!");

        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(errorObject);
        response.getWriter().write(json);

    }
}
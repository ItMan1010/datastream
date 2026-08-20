package com.itman.datastream.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * {@link CommUtils#appendUrlParam(String, String, String)} 单元测试
 */
public class CommUtilsTest {

    private static final String SOCKET_TIMEOUT = "socketTimeout";
    private static final String TIMEOUT_VALUE = "5000";

    @Test
    public void appendUrlParamMultiParamUrlEndsWithValue() {
        String url = "jdbc:mysql://127.0.0.1:13307/dbtest1?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        assertEquals(url + "&socketTimeout=5000",
                CommUtils.appendUrlParam(url, SOCKET_TIMEOUT, TIMEOUT_VALUE));
    }

    @Test
    public void appendUrlParamUrlEndsWithAmpersand() {
        String url = "jdbc:mysql://127.0.0.1:13307/dbtest1?useSSL=false&";
        assertEquals("jdbc:mysql://127.0.0.1:13307/dbtest1?useSSL=false&socketTimeout=5000",
                CommUtils.appendUrlParam(url, SOCKET_TIMEOUT, TIMEOUT_VALUE));
    }

    @Test
    public void appendUrlParamUrlEndsWithQuestionMark() {
        String url = "jdbc:mysql://127.0.0.1:13307/dbtest1?";
        assertEquals("jdbc:mysql://127.0.0.1:13307/dbtest1?socketTimeout=5000",
                CommUtils.appendUrlParam(url, SOCKET_TIMEOUT, TIMEOUT_VALUE));
    }

    @Test
    public void appendUrlParamUrlWithoutQuestionMark() {
        String url = "jdbc:mysql://127.0.0.1:13307/dbtest1";
        assertEquals("jdbc:mysql://127.0.0.1:13307/dbtest1?socketTimeout=5000",
                CommUtils.appendUrlParam(url, SOCKET_TIMEOUT, TIMEOUT_VALUE));
    }

    @Test
    public void appendUrlParamParamAlreadyExists() {
        String url = "jdbc:mysql://127.0.0.1:13307/dbtest1?socketTimeout=3000&useSSL=false";
        assertSame(url, CommUtils.appendUrlParam(url, SOCKET_TIMEOUT, TIMEOUT_VALUE));
    }

    @Test
    public void appendUrlParamNullUrl() {
        assertNull(CommUtils.appendUrlParam(null, SOCKET_TIMEOUT, TIMEOUT_VALUE));
    }

    @Test
    public void appendUrlParamEmptyUrl() {
        assertEquals("", CommUtils.appendUrlParam("", SOCKET_TIMEOUT, TIMEOUT_VALUE));
    }
}

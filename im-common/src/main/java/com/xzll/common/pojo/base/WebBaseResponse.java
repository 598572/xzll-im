package com.xzll.common.pojo.base;






import com.xzll.common.constant.answercode.AnswerCode;

import java.io.Serializable;

public class WebBaseResponse<T> implements Serializable{
	private static final long serialVersionUID = -1L;

	private int code;
	
	private String msg;

	private T data;

	public WebBaseResponse() {}

	public WebBaseResponse(int code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	/**
	 * 返回成功，沒有data值
	 */
	public static <T> WebBaseResponse<T> returnResultSuccess() {
		return setResult(AnswerCode.SUCCESS.getCode(), AnswerCode.SUCCESS.getMessage(), null);
	}

	/**
	 * 返回成功
	 *
	 * @param data
	 * @return
	 */
	public static <T> WebBaseResponse<T> returnResultSuccess(T data) {
		return setResult(AnswerCode.SUCCESS.getCode(), AnswerCode.SUCCESS.getMessage(), data);
	}

	/**
	 * 返回成功，沒有data值
	 *
	 * @param msg
	 * @return
	 */
	public static <T> WebBaseResponse<T> returnResultSuccess(String msg) {
		return returnResultSuccess(msg, null);
	}

	/**
	 * 返回成功
	 */
	public static <T> WebBaseResponse<T> returnResultSuccess(String msg, T data) {
		return setResult(AnswerCode.SUCCESS.getCode(), msg, data);
	}

	/**
	 * 返回失败, 可以指定code和msg
	 *
	 * @param code
	 * @param msg
	 * @return
	 */
	public static <T> WebBaseResponse<T> returnResultError(int code, String msg) {
		return setResult(code, msg, null);
	}

	/**
	 * 返回失败，可以传msg
	 *
	 * @param msg
	 * @return
	 */
	public static <T> WebBaseResponse<T> returnResultError(String msg) {
		return returnResultError(-200, msg);
	}

	/**
	 * 返回失败
	 * @param anwserCode
	 * @param <T>
	 * @return
	 */
	public static <T> WebBaseResponse<T> returnResultError(AnswerCode anwserCode) {
		if(anwserCode == null) {
			return returnResultError("操作失败");
		}

		return returnResultError(anwserCode.getCode(), anwserCode.getMessage());
	}

	/**
	 * 通用封装
	 *
	 * @param code 返回code
	 * @param msg  返回提示消息
	 * @param data 返回数据
	 * @return
	 */
	public static <T> WebBaseResponse<T> setResult(int code, String msg, T data) {
		return new WebBaseResponse(code, msg, data);
	}

	public static <T> WebBaseResponse<T> setResult(AnswerCode answerCode, T data) {
		return new WebBaseResponse(answerCode.getCode(), answerCode.getMessage(), data);
	}
	public static <T> WebBaseResponse<T> setResult(AnswerCode answerCode) {
		return new WebBaseResponse(answerCode.getCode(), answerCode.getMessage(), null);
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}


}

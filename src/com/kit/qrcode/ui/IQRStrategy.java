package com.kit.qrcode.ui;

/**
 * 
 * @ClassName QRStrategy
 * @Description 二维码策略（跳转界面等）
 * @author Zhao laozhao1005@gmail.com
 * @date 2014-6-10 上午8:53:21
 * 
 */
public interface IQRStrategy {
	
	public void qrWhereToGo(String content);
	
	public void qrDoSomething();

}

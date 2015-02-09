package com.otl.scheduler.mailerimport com.otl.reports.beans.MailTemplate;import com.otl.reports.beans.UserInfo;



public interface  Mailer
{
	/**	 * UserName password for 	 * @param userInfo	 */
	void loginMail(UserInfo userInfo);
	void attachFiles(def strFileLocation);
	def sendMail(MailTemplate mailTemplate);
	
}

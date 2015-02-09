package com.otl.scheduler.mailer;

import net.sourceforge.htmlunit.corejs.javascript.ast.CatchClause;

import com.gargoylesoftware.htmlunit.html.DomElement
import com.gargoylesoftware.htmlunit.html.HtmlFileInput
import com.gargoylesoftware.htmlunit.html.HtmlElement
import com.gargoylesoftware.htmlunit.html.HtmlInput
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import com.gargoylesoftware.htmlunit.javascript.host.Event
import com.otl.reports.controller.Configurator;
import com.otl.reports.beans.MailTemplate
import com.otl.reports.beans.UserInfo;
import com.otl.reports.model.WebBrowser;
import com.sun.corba.se.impl.oa.poa.ActiveObjectMap.Key;
import com.otl.reports.helpers.Log;

import groovy.json.JsonSlurper

public class OutlookLightMode implements Mailer{

	
	WebBrowser webBrowser=null
	
		def init(def proxy){
	
			webBrowser=new WebBrowser()
			webBrowser.init(proxy)
		}
	
		static def getstringbetween(String str,String start,String end){
	
			def resp=null
			try{
	
				resp=str.substring(str.indexOf(start)+start.length(),str.indexOf(end, str.indexOf(start)))
			}catch(Exception){
			}
			return resp
		}
		
		
		void loginMail(UserInfo userInfo){
			
			webBrowser.Navigate(Configurator.globalconfig.outlook_host + Configurator.globalconfig.outlook_url)
			
					//Login into Outlook mail using light mode
					webBrowser.typeOnName(Configurator.globalconfig.outlook_element_map.loginuser, userInfo.user)
					webBrowser.typeOnName(Configurator.globalconfig.outlook_element_map.loginpwd,  userInfo.password)
					//Checking if the light mode is enabled or not
					if(webBrowser.findElemById('chkBsc').checked)
					{
						println "Value of the Light Mode is : " + webBrowser.findElemById('chkBsc').checked
					}else{
						webBrowser.clickOnInputId('chkBsc')
						println "Value of the Light Mode after clicking is : " + webBrowser.findElemById('chkBsc').checked
					}
					
					DomElement loginButton
					String strElem
					loginButton = webBrowser.querySelector("input[type*=submit]")
										
					webBrowser.clickLink((HtmlInput)loginButton)
					webBrowser.waitForPageLoad()
					
		}
		void attachFiles(def strFileLocation){
			
		
				
				webBrowser.executeScriptforNewPage("onClkTb('attachfile');")
				DomElement elemFileUpload = webBrowser.findElemById("attach")
				Log.debug("Finding the browse button 1 : " + elemFileUpload.getNodeValue())
				((HtmlFileInput)elemFileUpload).setValueAttribute(strFileLocation)
				Log.debug("Uploading file from the following path : " + ((HtmlFileInput)elemFileUpload).valueAttribute)
				DomElement elemAttach =webBrowser.findElemById("attachbtn")
				webBrowser.clickLink((HtmlInput)elemAttach)
				Log.debug("Finding the browse button : " + elemFileUpload)
				webBrowser.executeScriptforNewPage("onClkTb('done');")
				Log.info("Report attached sucessfully : " + strFileLocation)
				webBrowser.executeScriptforNewPage("onClkTb('send');")
			
						
		}
		//TOD: Check why there is no mail received for invalid address
		def sendMail(MailTemplate mailTemplate){
			println "MailTemplate value : " + mailTemplate.userName
			UserInfo userInfo = new UserInfo()
			userInfo.user = mailTemplate.userName
			userInfo.password = mailTemplate.password
			//step 1: Login into outlook mail
			loginMail(userInfo)
			//step 2: Creating a new mail
			webBrowser.executeScriptforNewPage("onClkTb('newmsg');")
			if(webBrowser.findElemById("txtSch") != null)
			{
				try{
					Log.info("Sucessfully logged into the mail using light mode :" + webBrowser.findElemById("txtSch"))
					
					//Step 2.1 - Filling out TO,CC,BCC, Subject and Mail Content
					webBrowser.typeOnName("txtto", mailTemplate.toAddress)
					webBrowser.typeOnName("txtcc", mailTemplate.ccAddress)
					webBrowser.typeOnName("txtbcc", mailTemplate.bccAddress)
					webBrowser.typeOnName("txtsbj", mailTemplate.subject)
					DomElement elemMailBody = webBrowser.querySelector("textarea")
					Log.debug("Value of the email body : " + elemMailBody )
					
					((HtmlTextArea)elemMailBody).setText(mailTemplate.mailContent)
					Log.debug("Value of the email body : " + elemMailBody)
					
					//Step 2.2: Attaching file to the mail
					//This function is designed to attach only one attachment.
					attachFiles(mailTemplate.attachmentLocation)
										
					//Step 2.3: Finally sending the mail
					webBrowser.executeScriptforNewPage("onClkTb('send');")
					if(webBrowser.querySelector("div[class*='w100']")!=null)
					{
						Log.error("Mail sending failed, you have to look for the errors")
						return false
						
					}
					
					return true
				}catch(Exception e)
				{
					Log.error("Exception while sending the mail : " + e.printStackTrace())
					return false;
				}
				
				
				
			}
			else{
				
				Log.error("Not able to find the text search box, guess there is problem loggin in:")
				return false
				
			}
		}
		
		
		
	
		static parseconfig(def configFileName){
			
			// Read the configuration file into a map called "global".
			// This map is shared with all other threads in order to provide
			// a centralised configuration store.
			
			
			
			
			try
			{
				Configurator.globalconfig = new JsonSlurper().parse(new FileReader(configFileName))
			
				Configurator.globalconfig.configuration_file = configFileName
			
				println "Configuration: ${Configurator.globalconfig}"
			}
			catch(Exception e)
			{
				println "Error: Unable to load configuration"
				e.printStackTrace()
				System.exit(1)
			}
			
		}
		
	public static void main(String[] args) {
		println "Test Parser :"
		def configFileName
		
		configFileName=args[0]
		
		parseconfig(configFileName)
		
		
		// TODO Auto-generated method stub
		OutlookLightMode mailer = new OutlookLightMode()
		mailer.init(Configurator.globalconfig?.proxy)
		UserInfo userInfo = new UserInfo()
		userInfo.setUser(Configurator.globalconfig.outlook_username)
		userInfo.setPassword(Configurator.globalconfig.outlook_password)
		//userInfo.setUser("bsubramanian@corpuk.net")
		//userInfo.setPassword("Pin#12345")
		
		
		MailTemplate testMail = new MailTemplate()
		testMail.userName = Configurator.globalconfig.outlook_username
		testMail.password = Configurator.globalconfig.outlook_password
		testMail.toAddress = "balaji.subramanian@three.co.uk;"
		testMail.ccAddress = ""
		testMail.bccAddress = ""
		testMail.attachmentLocation = "C:\\Users\\hutchuk\\Desktop\\Automation framework.docx"
		testMail.mailContent = "This is sample report for the Automated test eamil"
		testMail.subject = "Automated test email"
		println "Success value of Mail sent :" + mailer.sendMail(testMail)
				
		//println "Test Parser :" + mailer.login(userInfo)
	}

}

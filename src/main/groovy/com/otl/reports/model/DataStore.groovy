package com.otl.reports.model

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentNavigableMap;
import java.sql.*

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.sqlite.SQLite

import groovy.sql.DataSet
import groovy.sql.Sql

import com.otl.reports.beans.TimeEntry
import com.otl.reports.beans.UserInfo
import com.otl.reports.beans.UserTimeSummary
import com.otl.reports.exceptions.ServiceException
import com.otl.reports.helpers.Log
import com.otl.reports.helpers.AppCrypt


class DataStore {


	Sql fetcherDB =null;
	Sql userDB =null;

	void close(){
		fetcherDB.close();
		userDB.close();
	}


	void init(def userfileName,def fetchfileName){

		Class.forName("org.sqlite.JDBC")

		fetcherDB = Sql.newInstance("jdbc:sqlite:"+fetchfileName, "org.sqlite.JDBC")
		userDB = Sql.newInstance("jdbc:sqlite:"+userfileName, "org.sqlite.JDBC")
		//create table if not exists TableName (col1 typ1, ..., colN typN)
		/*
		 public String user
		 def password
		 def ip
		 */
		
		
		userDB.execute("create table if not exists userInfo (user string, password string,ip string,locked string,lastupdated date)")

		/*
		 * Date entryDate
		 def user
		 def projectcode
		 def projecttask
		 def tasktype
		 def hours
		 def details
		 * 
		 */
		fetcherDB.execute("create table if not exists timeentry (user string, entryDate date,projectcode string,projecttask string,tasktype string,hours integer,details string,key string)")

		
		//println(fetcherDB.dump())
	}

	public ArrayList<UserInfo> getUserEntries(){


		ArrayList<UserInfo> userEntries=new ArrayList<UserInfo>()
		
		
		userDB.rows("select * from userInfo " ).each{

			userEntries.add(
					new UserInfo(
					user: it.user,
					password: AppCrypt.decrypt(it.password),
					ip: it.ip,
					locked: it.locked
					

					)
					);
		}

		return userEntries
	}
	
	public ArrayList<UserInfo> getValidUserEntries(){
		
		
				ArrayList<UserInfo> userEntries=new ArrayList<UserInfo>()
		
				userDB.rows("select * from userInfo where locked='false' " ).each{
		
					userEntries.add(
							new UserInfo(
							user: it.user,
							password: AppCrypt.decrypt(it.password),
							ip: it.ip,
							locked: it.locked
							
		
							)
							);
				}
		
				return userEntries
			}

	public UserInfo findUser(String user){

		UserInfo userInfo=null
		String cond=" where 1=1 "
		if(null != user)
			cond=cond + " AND user like '${user}' "
		else
			return null

		

		userDB.rows("select * from userInfo " + cond ).each{


			userInfo=new UserInfo(
					user: it.user,
					password: AppCrypt.decrypt(it.password),
					ip: it.ip,					
					locked: it.locked

					)
		}

		return userInfo
	}


	public ArrayList<UserTimeSummary> getuserstatusList(String user){
		ArrayList<UserTimeSummary> userstatuslist=new ArrayList<UserTimeSummary>()
			String cond=" where 1=1 "
				if(null != user)
					cond=cond + " AND user like '${user}' "
					
					userDB.rows("select user,locked from userInfo " + cond ).each{
						
									userstatuslist.add(
											new UserTimeSummary(											
											user: it.user,
											userLocked: Boolean.parseBoolean(it.locked)										
											)
											);
								}
					
		return userstatuslist
		
	}
	
	 long getWorkingDaysBetweenTwoDates(Date start, Date end) {
		
    Calendar c1 = GregorianCalendar.getInstance();
    c1.setTime(start);
    int w1 = c1.get(Calendar.DAY_OF_WEEK);
    c1.add(Calendar.DAY_OF_WEEK, -w1 + 1);

    Calendar c2 = GregorianCalendar.getInstance();
    c2.setTime(end);
    int w2 = c2.get(Calendar.DAY_OF_WEEK);
    c2.add(Calendar.DAY_OF_WEEK, -w2 + 1);

    //end Saturday to start Saturday 
    long days = (c2.getTimeInMillis()-c1.getTimeInMillis())/(1000*60*60*24);
    long daysWithoutSunday = days-(days*2/7);

    if (w1 == Calendar.SUNDAY) {
        w1 = Calendar.MONDAY;
    }
    if (w2 == Calendar.SUNDAY) {
        w2 = Calendar.MONDAY;
    }
    return daysWithoutSunday-w1+w2;
	}
	public def getTimesheetEntriesSummary(String user,Date from,Date to,def leavecodes){
		
		def timeEntries=new HashMap<String,UserTimeSummary>()
		
		//		ArrayList<UserTimeSummary> timeEntries=new ArrayList<UserTimeSummary>()
		
				String cond=" where 1=1 "
				if(null != user)
					cond=cond + " AND user like '${user}' "
		
		
				if(null != from )
					cond=cond + " AND entryDate >= "+ from.getTime()
		
				if(null != to)
					cond=cond + " AND entryDate <= "+ to.getTime()

					int duration=0
					if(null != from && null != to)
						duration=(getWorkingDaysBetweenTwoDates(from,to)) * 8
					
					String maxdateQuery="Select user,max(entryDate) as maxentrydate from timeentry ${cond}  group by user "					
					
					String workhrsQuery="Select user,total(hours) as totalhrs from timeentry ${cond} and projectcode not in($leavecodes)  group by user "
					
					String leavehrsQuery="Select user,total(hours) as totalhrs from timeentry ${cond} and projectcode  in($leavecodes)  group by user "
					
					
					//calculate total working hours 
					
					/*
					 * 
					println maxdateQuery
					println workhrsQuery
					println leavehrsQuery
					
					*/
					
					//Select user,max(entryDate)from timeentry group by user
		
					// group by user where projectcode not in[leavecodes]
					
					//Select user,total(hours) as totalhrs from timeentry group by user where projectcode in[ leavecodes]
					
					
					//Merge and Add entries
					//user, workinghours leavehours,lastupdated entry
		
					
					getuserstatusList( user).each{
						
							
								timeEntries.put(it.user, new UserTimeSummary(
									user: it.user,
									userLocked:it.userLocked
									))
								
								}
					
					
				fetcherDB.rows(maxdateQuery).each{
		
					
					timeEntries.get(it.user)?.lastupdated=it.new Date(it.maxentrydate)
					
				}
		
				fetcherDB.rows(workhrsQuery).each{
				
						timeEntries.get(it.user)?.workhours=it.totalhrs
					
						}
					
				fetcherDB.rows(leavehrsQuery).each{
					
							timeEntries.get(it.user)?.leavehours=it.totalhrs
						
							}
				
				timeEntries.each{timeEntry->
					
					int totalhrs=timeEntry.leavehours + timeEntry.workhours
							if(totalhrs < duration  )
								timeEntry.defaulter=true
							else
								timeEntry.defaulter=false
						
							}
				
				
				return timeEntries
				
			}
	
	
	
	
	
		//return dataStore.getUserEntries()
	//return dataStore.findUser(user)

	public ArrayList<TimeEntry> getTimesheetEntries(String user,Date from,Date to,String leavecodes){

		ArrayList<TimeEntry> timeEntries=new ArrayList<TimeEntry>()

		String cond=" where 1=1 "
		if(null != user)
			cond=cond + " AND user like '${user}' "


		if(null != from )
			cond=cond + " AND entryDate >= "+ from.getTime()

		if(null != to)
			cond=cond + " AND entryDate <= "+ to.getTime()

Log.info("Query select * from timeentry " + cond)
			
		fetcherDB.rows("select * from timeentry " + cond).each{

			
			
				boolean isLeave=false
					if(leavecodes.contains("" + it.projectcode))
						isLeave=true
			
			
			
			timeEntries.add(
					new TimeEntry(
					entryDate: new Date(it.entryDate),
					hours: it.hours,
					user: it.user,
					projectcode: it.projectcode,
					projecttask: it.projecttask,
					tasktype: it.tasktype,
					details: it.details,
					isLeave: isLeave, 
					fetchedDate: new Date() 
					)
					);
		}
		Log.info("Query returned" + timeEntries.size())
		return timeEntries
	}
	void insertTimesheet(TimeEntry timeEntry){


		boolean exists =false

		def query="select key from timeentry  where key='" +timeEntry.key +"'"

		exists=(fetcherDB.rows(query).size()>0)




		if( exists){
			Log.error("Key Already exists ${timeEntry.key} overWriting ");

			query="delete from timeentry  where key='${timeEntry.key}'"
			fetcherDB.execute(query, []);

		}

		DataSet curtimeEntry = fetcherDB.dataSet("timeentry")
		//user string, projectcode string,projecttask string,tasktype string,hours integer,details string,key string
		//	curtimeEntry.
		curtimeEntry.add(
				user:timeEntry.user,
				projectcode:timeEntry.projectcode,
				projecttask:timeEntry.projecttask,
				tasktype:timeEntry.tasktype,
				hours:timeEntry.hours,
				details:timeEntry.details,
				key:timeEntry.key,
				entryDate:timeEntry.entryDate.getTime()
				)




	}

	void printData(){

		println(userDB.rows("select * from userInfo").size())
		userDB.rows("select * from userInfo").each{ println(it) }


		println(fetcherDB.rows("select * from timeentry").size())

		fetcherDB.rows("select * from timeentry").each{ println(it) }


	}
	
	void updateUserLock(UserInfo userInfo){
		
		
		
				boolean exists =false
		
		
		
				def query="select user from userInfo  where user='" + userInfo.user + "'"
		
			
				exists=(userDB.rows(query).size()>0)
		
				// boolean exists = fetcherDB.execute("select user from userInfo  where user='${userInfo.user}'", null);
		
		
				if( exists){
		
					Log.error("Key Already exists ${userInfo.user} overWriting ");
					query="update userInfo set locked='${userInfo.locked}' where user='${userInfo.user}'"
					println(query)
					
					userDB.executeUpdate(query, []);
					
					return
		
				}else{
				
				throw new ServiceException("User did not exist in DB")
				}
		
		
		
			}
	
	
	
	void insertUser(UserInfo userInfo){



		boolean exists =false



		def query="select user from userInfo  where user='" + userInfo.user + "'"

		exists=(userDB.rows(query).size()>0)

		// boolean exists = fetcherDB.execute("select user from userInfo  where user='${userInfo.user}'", null);


		if( exists){

			Log.error("Key Already exists ${userInfo.user} overWriting ");
			query="delete from userInfo  where user='${userInfo.user}'"
			userDB.executeUpdate(query, []);
			
			

		}

		DataSet curUserInfo = userDB.dataSet("userInfo")
		//user string, projectcode string,projecttask string,tasktype string,hours integer,details string,key string

		curUserInfo.add(
				user:userInfo.user,
				password:AppCrypt.encrypt(userInfo.password),
				ip:userInfo.ip,
				locked:"false",
				lastupdated:new Date()

				)

		//curUserInfo.commit();
		//insert into myTable(colname1, colname2) values(?, ?)
		//	def res=fetcherDB.executeUpdate("insert into userInfo values(?,?,?)",[userInfo.user,userInfo.password,userInfo.ip]);
		//println(res)


	}
}

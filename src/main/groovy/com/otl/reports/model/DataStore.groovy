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

import com.gargoylesoftware.htmlunit.InsecureTrustManager2;
import com.otl.reports.beans.ProjectEmployeeReport
import com.otl.reports.beans.ProjectInfo
import com.otl.reports.beans.TimeEntry
import com.otl.reports.beans.TimesheetStatusReport
import com.otl.reports.beans.UserInfo
import com.otl.reports.beans.UserTimeSummary
import com.otl.reports.exceptions.ServiceException
import com.otl.reports.helpers.Log
import com.otl.reports.helpers.AppCrypt
import com.otl.reports.controller.Configurator


class DataStore {


	Sql fetcherDB =null;
	Sql userDB =null;
	Sql tmpImpDB = null;
	Sql jobDB = null;

	def users=[:]
	def projects=[:]

	void close(){
		fetcherDB.close();
		userDB.close();
		tmpImpDB?.close();
	}



	void init(def userfileName,def fetchfileName, def jobFileName){

		Class.forName("org.sqlite.JDBC")

		fetcherDB = Sql.newInstance("jdbc:sqlite:"+fetchfileName, "org.sqlite.JDBC")
		userDB = Sql.newInstance("jdbc:sqlite:"+userfileName, "org.sqlite.JDBC")
		jobDB = Sql.newInstance("jdbc:sqlite:"+jobFileName, "org.sqlite.JDBC")
		
		//create table if not exists TableName (col1 typ1, ..., colN typN)
		/*
		 public String user
		 def password
		 def ip
		 */
		
		/*
		 * 	def jobName
			def jobID
			def jobDesc
			def jobValidity
			def jobStartDate
			def jobEndDate
			def jobFrequency
			def jobRetry
			def jobReportFolder
			def jobToAddress
			def jobCCAddress
			def jobBCCAddress
			def jobSubject
			def jobMailContent
			def jobOwner
		 */
		//jobDB.execute("create table if not exists jobDescription (jobName string, jobID string,jobDesc string,jobValidity string,jobStartDate date, jobEndDate date,jobFrequency string, jobRetry integer, jobReportFolder string, jobToAddress string, jobCCAddress string, jobBCCAddress string, jobSubjectstring, jobMailContent string, jobOwner string)")
		jobDB.execute("create table if not exists jobdescription (jobname string, jobid string,jobdesc string,jobvalidity string,jobstartdate date, jobenddate date,jobfrequency string, jobretry integer, jobreportfolder string, jobtoaddress string, jobccaddress string, jobbccaddress string, jobsubjectstring, jobmailcontent string, jobowner string)")
		
		/*
		 *
		 *	def jobInstID
			def jobInstDesc
			def jobParentID
			def jobInstActualStartTime
			def jobInstRevisedStartTime
			def jobInstResult
			def jobInstRetry
			def jobInstReportLocation
			def jobInstReportName
		 */
		//jobDB.execute("create table if not exists jobInstance (jobInstID string ,jobInstDesc string , jobParentID string , jobInstActualStartTime Date,jobInstRevisedStartTime Date, jobInstResult string, jobInstRetry integer, jobInstReportLocation string, jobInstReportName string  )")
		jobDB.execute("create table if not exists jobinstance (jobinstid string ,jobinstdesc string , jobparentid string , jobinstactualstarttime date,jobinstrevisedstarttime date, jobinstresult string, jobinstretry integer, jobinstreportlocation string, jobinstreportname string  )")
		
		userDB.execute("create table if not exists userInfo (user string, password string,ip string,locked string,lastupdated date,team string,comment string)")

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
		fetcherDB.execute("create table if not exists timeentry (user string, entryDate date,projectcode string,projecttask string,tasktype string,hours integer,details string,key string,status string)")


		/*
		 *
		 def projectcode
		 def projectname
		 def projectid
		 *
		 */
		fetcherDB.execute("create table if not exists projectdetails (code string,name string,projectid string)")

		updateUserCache();
		updateProjectCache();
		//println(fetcherDB.dump())
	}
	
	
	public def getDBHandler(def importfileName){
		
		//Split the function to get the db handler.
		try{
			if(importfileName != null && importfileName!= ""){
				tmpImpDB = Sql.newInstance("jdbc:sqlite:"+importfileName, "org.sqlite.JDBC")
				}
		}
		catch(Exception e){
			Log.error("Error while obtaining database connection of importing db file. " + e.printStackTrace())
		}
		
		return tmpImpDB
	}

	public def getDataFromDB(def curdbobj, def tableName){
		
		//Get the data from the 
		if(curdbobj == null){
			Log.error("Database connection is null and data arraylist cannont be retrieved. ")
		}
		else{
			println "Value of the curdbobj : " + curdbobj
		}
		ArrayList recordList = null
		if( tableName == "timeentry" ){
			Log.info( "Obtaining record set from timeentry table")
			recordList = curdbobj.rows("select * from timeentry")
		}else if( tableName == "projectdetails" ){
			Log.info( "Obtaining record set from projectdetails table")
			recordList = curdbobj.rows("select * from projectdetails")
		}else if( tableName == "userInfo" ){
			Log.info( "Obtaining record set from userInfo table")
			recordList = curdbobj.rows("select * from userInfo")
		}
		//println "Value of the record list : " + recordList
		return recordList
		
	}
	
	public def backupCurrentDB(){
		
		def BACKUP_DIRECTORY = "C:/uk/backup"
		BACKUP_DIRECTORY = Configurator.globalconfig.dbbkp_path
		try{
			FileOutputStream fosFetcherDB = new FileOutputStream(new File(Configurator.globalconfig.dbbkp_path+"\\otlfetcher.db_"+new Date().getTime()+"_bkp"))
			InputStream fisFetcherDB = new FileInputStream(new File(Configurator.globalconfig.fetcherdb))
			fosFetcherDB << fisFetcherDB
			fisFetcherDB.close()
			fosFetcherDB.close()
			FileOutputStream fosUserDB = new FileOutputStream(new File(Configurator.globalconfig.dbbkp_path+"\\otluser.db_"+new Date().getTime()+"_bkp"))
			InputStream fisUserDB = new FileInputStream(new File(Configurator.globalconfig.userdb))
			fosUserDB << fisUserDB
			fosUserDB.close()
			fisUserDB.close()
		}
		catch(Exception e){
			Log.error("Error while backing up the database. " + e.printStackTrace())
			return false
		}
		
		return true
	}

	
	public def importDBRecords(def tableName, def chunkedRecords , def dboverride)
	{
		TimeEntry tmpTimeEntry = null
		ProjectInfo tmpProjectInfo = null
		UserInfo tmpUserInfo = null
		String encryptFlag = "disabled"
		def response = "Failure in importing database : Some exception happened"
		try{
						
			if(tableName==null || tableName == "" || chunkedRecords == "" || chunkedRecords == null){
				Log.error("Invalid table name selected for import. TableName or recordList seems to be null or empty")
				response = "Failure in importing database : Invalid Table Name"
			}
			else if(tableName == "timeentry"){
				
				chunkedRecords.each{
										
					tmpTimeEntry = new TimeEntry(
							entryDate: new Date(it.entryDate) ,
							user:it.user,
							projectcode:it.projectcode,
							projecttask:it.projecttask,
							tasktype:it.tasktype,
							hours:it.hours,
							details:it.details,
							//isLeave:it.isLeave,
							//fetchedDate:it.fetchedDate,
							status:it.status,
							//team:it.team,
							//projectInfo:it.projectInfo

							)

					insertTimesheet(tmpTimeEntry,dboverride)
					Log.debug( it.toString())
					tmpTimeEntry = null
				}
				
				response = "Successfully imported records to database: timeentry database"
				
			}
			else if(tableName == "projectdetails"){
				
				chunkedRecords.each{
					
					tmpProjectInfo = new ProjectInfo(
							name: it.name,
							code: it.code,
							projectid: it.projectid

							)
					insertProject(tmpProjectInfo,dboverride)
					Log.debug( it.toString())

					tmpProjectInfo = null
				}
				response = "Successfully imported records to database: projectdetails database"
			}
			else if(tableName == "userInfo"){
				
				chunkedRecords.each{
						tmpUserInfo = new UserInfo(
							user: it.user,
							password: it.password,
							ip: it.ip,
							locked: it.locked,
							team:it?.team	);
					Log.debug( it.toString())
					insertUser(tmpUserInfo, dboverride, encryptFlag)
					tmpUserInfo = null
				}
				response = "Successfully imported records to database: userInfo database"
			
			}
			else{
				
				Log.error("Invalid Table name. Following table Name is not available in OTL schema." + tableName)
				response = "Failure in importing database : Some exception happened"
			}
		} catch(Exception e){
			
			response = "Failure in importing database : Some exception happened"
		}
		return response
	}
	
	// get db instantce() => dbinstance , tablename
	// get table as arraylist (dbinstance ,tablename)
	/**
	 * This funciton is to import the OTL database.
	 * @deprecated - This function is deprecated. As we have moved on to Asynchronous thread based db import.
	 * @param request
	 * @return
	 */
	public def importDB(def importfileName, String override="enabled"){

		def BACKUP_DIRECTORY = "C:/uk/backup"
		BACKUP_DIRECTORY = Configurator.globalconfig.dbbkp_path
		try{

			if(override == null || override == "") {
				override = "disabled"
			}
			//println "Import operation has commenced with Override being " + override
			TimeEntry tmpTimeEntry = null
			ProjectInfo tmpProjectInfo = null
			UserInfo tmpUserInfo = null
			//Iterating to import the values from the imported database
			if(importfileName != null && importfileName!= ""){
				tmpImpDB = Sql.newInstance("jdbc:sqlite:"+importfileName, "org.sqlite.JDBC")
				Log.debug("Taking backup of the database before import")


				//	new File(new File(Configurator.globalconfig.dbbkp_path+"\\otlfetcher.db_bkp_"+new Date().getTime())) << new File(new File(Configurator.globalconfig.fetcherdb)).bytes
				//	new File(Configurator.globalconfig.dbbkp_path+"\\otluser.db_bkp_"+new Date().getTime()) << new File(new File(Configurator.globalconfig.userdb)).bytes


				FileOutputStream fosFetcherDB = new FileOutputStream(new File(Configurator.globalconfig.dbbkp_path+"\\otlfetcher.db_"+new Date().getTime()+"_bkp"))
				InputStream fisFetcherDB = new FileInputStream(new File(Configurator.globalconfig.fetcherdb))
				fosFetcherDB << fisFetcherDB
				fisFetcherDB.close()
				fosFetcherDB.close()
				FileOutputStream fosUserDB = new FileOutputStream(new File(Configurator.globalconfig.dbbkp_path+"\\otluser.db_"+new Date().getTime()+"_bkp"))
				InputStream fisUserDB = new FileInputStream(new File(Configurator.globalconfig.userdb))
				fosUserDB << fisUserDB
				fosUserDB.close()
				fisUserDB.close()

			}
			else
			{
				Log.error("Invalid import file name ")
			}
			String encryptFlag = "disabled"
			String qryListTableName =
					tmpImpDB.rows("SELECT tbl_name FROM sqlite_master UNION SELECT tbl_name FROM sqlite_temp_master;").each{
						if( it.tbl_name == "timeentry" ){
							Log.debug( "Import started from the timeentry table")
							tmpImpDB.rows("select * from timeentry").each {
								tmpTimeEntry = new TimeEntry(
										entryDate: new Date(it.entryDate) ,
										user:it.user,
										projectcode:it.projectcode,
										projecttask:it.projecttask,
										tasktype:it.tasktype,
										hours:it.hours,
										details:it.details,
										//isLeave:it.isLeave,
										//fetchedDate:it.fetchedDate,
										status:it.status,
										//team:it.team,
										//projectInfo:it.projectInfo

										)

								insertTimesheet(tmpTimeEntry,override)
								Log.debug( it.toString())
								tmpTimeEntry = null
							}
						}
						else if(it.tbl_name == "projectdetails"){
							Log.debug( "Import started from the projectdetails table")
							tmpImpDB.rows("select * from projectdetails").each {
								tmpProjectInfo = new ProjectInfo(
										name: it.name,
										code: it.code,
										projectid: it.projectid

										)
								insertProject(tmpProjectInfo,override)
								Log.debug( it.toString())

								tmpProjectInfo = null
							}
						}
						else if(it.tbl_name == "userInfo"){
							Log.debug( "Import started from the userInfo table")
							tmpImpDB.rows("select * from userInfo").each {

								tmpUserInfo = new UserInfo(
										user: it.user,
										password: it.password,
										ip: it.ip,
										locked: it.locked,
										team:it?.team	);
								Log.debug( it.toString())
								insertUser(tmpUserInfo, override, encryptFlag)
								tmpUserInfo = null
							}
						}
						else{
							Log.error( "Not able to identify the db name.")
						}
					}


		}catch(Exception e){
			Log.error(e.printStackTrace())
		}

	}


	public def executeSQL( def db, def sqlString){

		def result=false
		if(db.equals("USER")){

			result= userDB.execute(sqlString)
		}

		if(db.equals("FETCHER")){
			result=fetcherDB.execute(sqlString)

		}

		if(db.equals())
			return result

	}


	void updateUserCache(){

		def tmpusers=[:]

		userDB.rows("select * from userInfo " ).each{

			tmpusers.put(it.user, new UserInfo(
					user: it.user,
					ip: it.ip,
					locked: it.locked,
					team:it?.team

					));




		}

		users=tmpusers;
	}


	public void insertProject(ProjectInfo projectInfo, String override="enabled"){



		boolean exists =false

		def query="select code from projectdetails  where code='" + projectInfo.code + "'"

		exists=(fetcherDB.rows(query).size()>0)

		// boolean exists = fetcherDB.execute("select user from userInfo  where user='${userInfo.user}'", null);


		if( exists){

			if(override == "enabled"){
				Log.error("Key Already exists ${projectInfo.code} overWriting ");
				query="delete from projectdetails  where code='${projectInfo.code}'"
				fetcherDB.executeUpdate(query, []);
				DataSet projectDataSet = fetcherDB.dataSet("projectdetails")

				projectDataSet.add(
						name:projectInfo.name,
						code:projectInfo.code,
						projectid:projectInfo.projectid

						)



			}
			else if(override == "disabled"){

				Log.error("Skipping the import for this record as Key Already exists for ${projectInfo.code}. ");
			}



		}
		else {
			DataSet projectDataSet = fetcherDB.dataSet("projectdetails")

			projectDataSet.add(
					name:projectInfo.name,
					code:projectInfo.code,
					projectid:projectInfo.projectid

					)

		}


		updateProjectCache();


	}
	void updateProjectCache(){


		def tmpprojects=[:]

		fetcherDB.rows("select * from projectdetails " ).each{



			tmpprojects.put(it.code, new ProjectInfo(
					name: it.name,
					code: it.code,
					projectid: it.projectid

					));




		}

		projects=tmpprojects;
		//	println(projects.dump())

	}

	//Get projectcodes which does not have entry in projectdetails
	public def getOrphanProjectCodes(){


		def projectcodes=[]
		updateProjectCache();

		fetcherDB.rows("Select projectcode from timeentry  group by projectcode").each{

			if(null == projects.getAt(it.projectcode))
				projectcodes.push(it.projectcode)



		}

		return projectcodes

	}
	public ArrayList<UserInfo> getUserEntries(String team){


		ArrayList<UserInfo> userEntries=new ArrayList<UserInfo>()


		userDB.rows("select * from userInfo order by team " ).each{

			boolean canadd=true

			if(null != team &&  team.equals(it?.team) == false){

				canadd=false
			}
			if(canadd){
				userEntries.add(
						new UserInfo(
						user: it.user,
						password: AppCrypt.decrypt(it.password),
						ip: it.ip,
						locked: it.locked,
						team:it?.team

						)
						);
			}
		}

		return userEntries
	}

	public ArrayList<UserInfo> getValidUserEntries(){


		ArrayList<UserInfo> userEntries=new ArrayList<UserInfo>()

		userDB.rows("select * from userInfo where locked='false' order by team " ).each{

			userEntries.add(
					new UserInfo(
					user: it.user,
					password: AppCrypt.decrypt(it.password),
					ip: it.ip,
					locked: it.locked,
					team:it?.team


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
					locked: it.locked,
					team:it?.team

					)
		}

		return userInfo
	}

	public boolean deleteUser(String user){



		if(null == user)

			return false



		Log.error("Deleting  ${user} overWriting ");
		def query="delete from userInfo  where user=:user"
		userDB.executeUpdate(query, ["user":user]);




		return true
	}



	public ArrayList<UserTimeSummary> getuserstatusList(String user){
		ArrayList<UserTimeSummary> userstatuslist=new ArrayList<UserTimeSummary>()
		String cond=" where 1=1 "
		if(null != user)
			cond=cond + " AND user like '${user}' "


		userDB.rows("select user,locked,team from userInfo " + cond +" order by team" ).each{

			userstatuslist.add(
					new UserTimeSummary(
					user: it.user,
					userLocked: Boolean.parseBoolean(it.locked),
					team:it?.team
					)
					);
		}

		Log.debug("select user,locked,team from userInfo " + cond)
		Log.debug(userstatuslist.size())
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






	public def getProjectHoursReport(String projectcode,Date from,Date to){

		//def timeEntries=new HashMap<String,ProjectEmployeeReport>()

		ArrayList<ProjectEmployeeReport> lstProjectEmployeeReport=new ArrayList<ProjectEmployeeReport>()

		String cond=" where 1=1 "
		if(null != projectcode)
			cond=cond + " AND projectcode like '${projectcode}' "


		if(null != from )
			cond=cond + " AND entryDate >= "+ from.getTime()

		if(null != to)
			cond=cond + " AND entryDate <= "+ to.getTime()





		String totalqry="Select user,projectcode,total(hours) as totalhrs from timeentry ${cond}  group by user,projectcode "


		fetcherDB.rows(totalqry).each{
			def userteam=getUserTeam(it.user)

			lstProjectEmployeeReport.add( new ProjectEmployeeReport(
					user: it.user,
					projectcode:it.projectcode,
					totalhrs: it.totalhrs,
					team: userteam,
					projectInfo:projects.getAt(it.projectcode)

					))




		}



		return lstProjectEmployeeReport

	}

	def getUserTeam(def user){

		return users.getAt(user)?.team;
	}
	public def getProjectEmployeeReport(String projectcode,Date from,Date to){

		//def timeEntries=new HashMap<String,ProjectEmployeeReport>()

		ArrayList<ProjectEmployeeReport> lstProjectEmployeeReport=new ArrayList<ProjectEmployeeReport>()

		String cond=" where 1=1 "
		if(null != projectcode)
			cond=cond + " AND projectcode like '${projectcode}' "


		if(null != from )
			cond=cond + " AND entryDate >= "+ from.getTime()

		if(null != to)
			cond=cond + " AND entryDate <= "+ to.getTime()



		String reportqry="Select user,projectcode,entryDate,hours,projecttask,tasktype from timeentry ${cond}   "

		String totalqry="Select user,projectcode,total(hours) from timeentry ${cond}  group by user,projectcode "


		fetcherDB.rows(reportqry).each{


			def userteam=getUserTeam(it.user)

			lstProjectEmployeeReport.add( new ProjectEmployeeReport(
					user: it.user,
					projectcode:it.projectcode,
					entryDate: new Date(it.entryDate),
					hours: it.hours,
					projecttask:it.projecttask,
					tasktype:it.tasktype,
					team:userteam,
					projectInfo:projects.getAt(it.projectcode)
					))




		}



		return lstProjectEmployeeReport

	}


	public def getProjectMonthlyReport(String projectcode, def users, Date from, Date to)
	{
		ArrayList<ProjectEmployeeReport> lstProjectHourlyReport=new ArrayList<ProjectEmployeeReport>()




		def nextFromDate
		def currStartDate
		def currEndDate
		def startDate = from
		def endDate = to
		Calendar c1 = GregorianCalendar.getInstance();
		c1.setTime(startDate);
		String userList = ""
		if(null!=users) {
			if(users!=null)
				userList = users.replaceAll(',' , '\',\'')
			userList = "('" + userList + "')"
		}
		Log.debug(userList)
		//println "Getting the time from calendar : " + c1.getTime()
		int w1 = c1.get(Calendar.DAY_OF_MONTH);
		//Testing the new logic, based on number of days in a month.
		String month
		nextFromDate = startDate

		while ( nextFromDate <= endDate)
		{
			c1.setTime(nextFromDate)
			w1 = c1.get(Calendar.DAY_OF_MONTH)
			int daysLeftInCurrentMonth=c1.getActualMaximum(GregorianCalendar.DAY_OF_MONTH) - w1
			//println "Number of days left in the month :" + daysLeftInCurrentMonth
			currStartDate = c1.getTime()
			currEndDate = c1.getTime() + daysLeftInCurrentMonth
			Log.debug( "Current Start Date in the month :" + currStartDate)
			Log.debug( "Current End Date in the month :" + currEndDate)
			nextFromDate = currEndDate + 1
			//println "nextFromDate computed is: " + nextFromDate
			if(currEndDate > endDate) { currEndDate = endDate}
			//println "Updated End Date in the month for last cycle :" + currEndDate
			month = "" + c1.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
			Log.debug( month)


			String cond=" where 1=1 "
			if(null != projectcode)
				cond=cond + " AND projectcode like '${projectcode}' "

			if(null != currStartDate )
				cond=cond + " AND entryDate >= "+ currStartDate.getTime()

			if(null != currEndDate)
				cond=cond + " AND entryDate <= "+ currEndDate.getTime()

			if(null!=users && users!='')
				cond = cond + " AND user in " + userList

			String totalqry="Select projectcode , '${month}' as month , total(hours) as totalhrs from timeentry ${cond}  group by projectcode"

			//println totalqry

			fetcherDB.rows(totalqry).each{
				// def userteam=findUser(it.user)?.team

				lstProjectHourlyReport.add( new ProjectEmployeeReport(

						projectcode:it.projectcode,
						//user: it.user,
						month: it.month,
						totalhrs: it.totalhrs,
						//team: userteam

						))


			}

		}

		//println "lstProjectHourlyReport value is:  " + lstProjectHourlyReport
		return lstProjectHourlyReport
	}

	public String getProjectName(String projectCode){
		String projectName

		String projectInfoQuery = "select name from projectdetails where code=${projectCode}"
		fetcherDB.rows(projectInfoQuery).each{

			projectName = it.name
			//println "Inside the ech loop - name : " + it.name

		}

		// "ProjectName value is: " + projectName
		if(projectName == null)
		{
			projectName = ""
		}
		return projectName


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


		Log.debug("Working days $from $to [ $duration]")
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
					userLocked:it.userLocked,
					leavehours: 0,
					workhours: 0
					))

		}


		fetcherDB.rows(maxdateQuery).each{


			timeEntries.get(it.user)?.lastupdated=new Date(it.maxentrydate)

		}

		fetcherDB.rows(workhrsQuery).each{

			timeEntries.get(it.user)?.workhours=it.totalhrs

		}

		fetcherDB.rows(leavehrsQuery).each{

			timeEntries.get(it.user)?.leavehours=it.totalhrs

		}

		timeEntries.each{key,timeEntry->



			int totalhrs=timeEntry.leavehours + timeEntry.workhours
			if(totalhrs < duration  )
				timeEntry.defaulter=true
			else
				timeEntry.defaulter=false

		}


		return timeEntries

	}





	public ArrayList<TimesheetStatusReport>  getWeeklystatus(def users,Date from,Date to){

		def timeEntries=new ArrayList<TimesheetStatusReport>()

		//		ArrayList<UserTimeSummary> timeEntries=new ArrayList<UserTimeSummary>()



		users.each{ user ->
			def startDate = from
			def endDate = to
			Calendar c1 = GregorianCalendar.getInstance();
			c1.setTime(startDate);
			int w1 = c1.get(Calendar.DAY_OF_WEEK);
			def nextFromDate = c1.getTime()
			def currStartDate
			def currEndDate
			Log.debug( "NexFromDate :" + nextFromDate)
			Log.debug("endDate :" + endDate)

			nextFromDate = startDate


			while ( nextFromDate <= endDate)
			{
				Log.debug( "Inside While Loop")
				c1.setTime(nextFromDate)
				w1 = c1.get(Calendar.DAY_OF_WEEK)
				int daysInCurrentWeek=Calendar.SATURDAY - w1 + 1
				//println "Number of days left in the week :" + daysInCurrentWeek
				currStartDate = c1.getTime()
				currEndDate = c1.getTime() + daysInCurrentWeek
				//println "Current Start Date in the week :" + currStartDate
				//println "Current End Date in the week :" + currEndDate
				nextFromDate = currEndDate + 1
				//println "nextFromDate computed is: " + nextFromDate
				if(currEndDate > endDate) { currEndDate = endDate}
				//println "Updated End Date in the week for last cycle :" + currEndDate


				String cond=" where 1=1 "

				if(null != user)
					cond=cond + " AND user like  '$user'"

				if(null != from )
					cond=cond + " AND entryDate >= "+ currStartDate.getTime()

				if(null != to)
					cond=cond + " AND entryDate <= "+ currEndDate.getTime()


				def statuslist=[]
				def totalhrs=0
				fetcherDB.rows("select user,status,total(hours) as totalhrs from timeentry " + cond +" group by user,status").each{

					if(statuslist.contains(it.status) == false){
						statuslist.push(it.status)

					}
					totalhrs +=it.totalhrs

				}

				Log.debug("select user,status,total(hours) as totalhrs from timeentry " + cond +" group by user,status")

				if(statuslist.size() > 0){
					def curstatus=statuslist.join(",")
					def userteam=getUserTeam(user);
					//	println(curstatus)
					timeEntries.push(

							new TimesheetStatusReport(
							user: user,
							status: curstatus,
							startdate: currStartDate,
							enddate: currEndDate ,
							team: userteam,
							totalhrs:totalhrs
							)

							)


					Log.info("Query returned" + timeEntries.size())

				}
			}
		}


		return timeEntries

	}


	//return dataStore.getUserEntries()
	//return dataStore.findUser(user)




	public ArrayList<TimeEntry> getProjectsList(String projectcode){

		ArrayList<TimeEntry> timeEntries=new ArrayList<TimeEntry>()

		String cond=" where 1=1 "
		if(null != projectcode)
			cond=cond + " AND projectcode like '${projectcode}' "



		fetcherDB.rows("select projectcode from timeentry " + cond + " group by projectcode").each{






			timeEntries.add(
					new TimeEntry(

					projectcode: it.projectcode,
					projectInfo:projects.getAt(it.projectcode)
					)
					);

		}
		Log.info("Query returned" + timeEntries.size())
		return timeEntries
	}



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
					projectInfo:projects.getAt(it.projectcode),
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

	void deleteTimesheet(def user,Date startdate,Date enddate){


		if(null == user || null == startdate || null == enddate)
			throw new ServiceException("Incorrect arguements");



		Log.error("Deleting timesheet for $user from $startdate to $enddate");

		def query="delete from timeentry  where user='${user}'  AND entryDate >= "+ startdate.getTime() + " AND entryDate <= "+ enddate.getTime()
		fetcherDB.execute(query, []);



	}
	void insertTimesheet(TimeEntry timeEntry, String override="enabled"){


		boolean exists =false

		def query="select key from timeentry  where key='" +timeEntry.key +"'"

		exists=(fetcherDB.rows(query).size()>0)

		Log.debug( "Override key is set to : " + override)


		if( exists){
			if(override == "enabled")
			{
				Log.error("Key Already exists ${timeEntry.key} overWriting ");
				query="delete from timeentry  where key='${timeEntry.key}'"
				fetcherDB.execute(query, []);

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
						entryDate:timeEntry.entryDate.getTime(),
						status:timeEntry.status
						)
			}

			else if(override=="disabled")
			{
				Log.error("Key Already exists ${timeEntry.key} and hence skipping the import ")

			}
		}
		else {

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
					entryDate:timeEntry.entryDate.getTime(),
					status:timeEntry.status
					)

		}



	}


	void printData(){

		Log.debug(userDB.rows("select * from userInfo").size())
		userDB.rows("select * from userInfo").each{ println(it) }


		Log.debug(fetcherDB.rows("select * from timeentry").size())

		fetcherDB.rows("select * from timeentry").each{ Log.debug(it) }


	}

	void updateUserLock(UserInfo userInfo){



		boolean exists =false



		def query="select user from userInfo  where user='" + userInfo.user + "'"


		exists=(userDB.rows(query).size()>0)

		// boolean exists = fetcherDB.execute("select user from userInfo  where user='${userInfo.user}'", null);


		if( exists){

			Log.error("Key Already exists ${userInfo.user} overWriting ");
			query="update userInfo set locked='${userInfo.locked}' where user='${userInfo.user}'"
			Log.debug(query)

			userDB.executeUpdate(query, []);

			return

		}else{

			throw new ServiceException("User did not exist in DB")
		}



	}



	void insertUser(UserInfo userInfo, String override="enabled", String encryptFlag="enabled"){



		boolean exists = false


		Log.debug("Value of the override parameter is : " + override)
		def query="select user from userInfo  where user='" + userInfo.user + "'"

		exists=(userDB.rows(query).size()>0)

		// boolean exists = fetcherDB.execute("select user from userInfo  where user='${userInfo.user}'", null);


		if( exists){

			if(override == "enabled"){

				Log.error("Key Already exists ${userInfo.user} overWriting ");
				query="delete from userInfo  where user='${userInfo.user}'"
				userDB.executeUpdate(query, []);

				DataSet curUserInfo = userDB.dataSet("userInfo")
				//user string, projectcode string,projecttask string,tasktype string,hours integer,details string,key string
				if(encryptFlag == "enabled"){
					curUserInfo.add(
							user:userInfo.user,
							password:AppCrypt.encrypt(userInfo.password),
							ip:userInfo.ip,
							locked:"false",
							lastupdated:new Date(),
							team:userInfo.team

							)
				}
				else if(encryptFlag == "disabled"){
					Log.debug( "Importing user from different db, hence skipping the encryption")
					curUserInfo.add(
							user:userInfo.user,
							password:userInfo.password,
							ip:userInfo.ip,
							locked:"false",
							lastupdated:new Date(),
							team:userInfo.team

							)

				}


			}else if(override == "disabled"){
				Log.error("Key Already exists ${userInfo.user}, hence skipping the import ");
			}


		}
		else
		{
			DataSet curUserInfo = userDB.dataSet("userInfo")
			//user string, projectcode string,projecttask string,tasktype string,hours integer,details string,key string

			if(encryptFlag == "enabled"){
				curUserInfo.add(
						user:userInfo.user,
						password:AppCrypt.encrypt(userInfo.password),
						ip:userInfo.ip,
						locked:"false",
						lastupdated:new Date(),
						team:userInfo.team

						)
			}
			else if(encryptFlag == "disabled"){
				Log.debug( "Importing user from different db, hence skipping the encryption")
				curUserInfo.add(
						user:userInfo.user,
						password:userInfo.password,
						ip:userInfo.ip,
						locked:"false",
						lastupdated:new Date(),
						team:userInfo.team

						)
			}

		}


		updateUserCache();
		//curUserInfo.commit();
		//insert into myTable(colname1, colname2) values(?, ?)
		//	def res=fetcherDB.executeUpdate("insert into userInfo values(?,?,?)",[userInfo.user,userInfo.password,userInfo.ip]);
		//println(res)


	}
}

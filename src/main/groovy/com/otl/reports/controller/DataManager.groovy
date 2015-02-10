package com.otl.reports.controller

import java.util.Date;

import com.otl.reports.beans.TimesheetStatusReport;



import com.otl.reports.beans.ProjectInfo
import com.otl.reports.beans.TimeEntry
import com.otl.reports.beans.UserInfo
import com.otl.reports.beans.UserTimeSummary;
import com.otl.reports.model.DataStore

import java.util.ArrayList;
import java.util.TreeMap.Entry

class DataManager {

	DataStore dataStore=null





	public void init(){


		dataStore=new DataStore()
		dataStore.init(Configurator.globalconfig.userdb, Configurator.globalconfig.fetcherdb, Configurator.globalconfig.jobdb)
	}

	def print(){

		dataStore.printData();
	}

	public def getDBHandler(def importdb_location){
		
		dataStore.getDBHandler(importdb_location)
		
	}
	
	public def backupCurrentDB(){
		
		dataStore.backupCurrentDB()
	}
	
	public def getDataFromDB(def curdbobj, def tableName){
		
		return dataStore.getDataFromDB(curdbobj,tableName)
	}
	
	public def importDBRecords(def tableName, def chunkedRecords , def dboverride){
		
		dataStore.importDBRecords(tableName, chunkedRecords, dboverride)
		
	}
	/**
	 * This function is to import the OTL database.
	 * @deprecated - This function is deprecated. As we have moved on to Asynchronous thread based db import. 
	 * @param request
	 * @return
	 */
	public def importDB(def importfileName, String override){
		dataStore.importDB(importfileName, override)
	}


	public void addTimeEntries(ArrayList<TimeEntry> timeEntries){


		def usertimemap=[:]
		for(TimeEntry timeEntry:timeEntries){

			def curtimeentries=[]
			if(usertimemap.containsKey(timeEntry.user) == false){

				curtimeentries.push(timeEntry)
			}else{
				curtimeentries=usertimemap[timeEntry.user]
				curtimeentries.push(timeEntry)
			}
			usertimemap[timeEntry.user]=curtimeentries
		}
		usertimemap.each {curuser,lstentries->

			lstentries.sort{it.entryDate}


			dataStore.deleteTimesheet(curuser,lstentries[0].entryDate,lstentries[lstentries.size()-1].entryDate)
		}



		for(TimeEntry timeEntry:timeEntries){
			dataStore.insertTimesheet(timeEntry,"enabled" )
		}
	}

	public void enableUser(String user){

		dataStore.updateUserLock(
				new UserInfo(
				user: user,
				locked: "false"
				)
				)
	}
	public void disableUser(String user){

		dataStore.updateUserLock(
				new UserInfo(
				user: user,
				locked: "true"
				)
				)
	}
	public void addUserEntries(UserInfo userInfo,String override, String encryptFlag){

		dataStore.insertUser(userInfo,override,encryptFlag)
	}


	public void insertProjects(def projectdetails, String override){

		for (ProjectInfo projectInfo:projectdetails){

			dataStore.insertProject(projectInfo, override)
		}

		dataStore.updateProjectCache();
	}


	public ArrayList<UserInfo> getValidUserEntries(){

		return dataStore.getValidUserEntries()
	}

	public def getOrphanProjectCodes(){

		return dataStore.getOrphanProjectCodes()
	}
	public ArrayList<TimesheetStatusReport>  getWeeklystatus(def users,Date from,Date to){

		return dataStore.getWeeklystatus(users,from,to)
	}

	public def getProjectMonthlyReport(String project, def users, Date from, Date to) {
		return dataStore.getProjectMonthlyReport(getvalidString(project),users,from,to)
	}

	public String getProjectName(String projectCode) {
		//String projectName = dataStore.getProjectName(projectCode)
		//println "ProjectName value in DM:  " +  projectName
		//return projectName
		return dataStore.getProjectName(projectCode)
	}

	public ArrayList<UserInfo> getUserEntries(){

		return dataStore.getUserEntries(null)
	}

	public ArrayList<UserInfo> getTeamUserEntriesasCSV(String team){

		return dataStore.getUserEntries(getvalidString(team))
	}

	public ArrayList<UserInfo> getUserEntries(String team){

		return dataStore.getUserEntries(getvalidString(team))
	}

	public ArrayList<UserTimeSummary> getAllUserStatus(){

		return dataStore.getuserstatusList(null)
	}

	public ArrayList<TimeEntry> getAllProjects(){

		return dataStore.getProjectsList(null)
	}

	public def executeSQL(def db,def sql){

		return dataStore.executeSQL( db, sql)
	}


	public UserInfo findUser(String user){


		return dataStore.findUser(user)
	}
	public boolean deleteUser(String user){


		return dataStore.deleteUser(user)
	}

	private String getProjectLeaveCodes(){
		String leavecode=""
		int i=0
		for(def bfr:Configurator.globalconfig.leavecodes){



			if(i > 0)
				leavecode=leavecode +","



			leavecode=leavecode +"'$bfr'"

			i++
		}
		return leavecode
	}

	public getvalidString(def user){

		if(null == user || user.trim() =="" || user.trim() =="null")
			return null

		return user
	}
	public def getTimesheetEntriesSummary(String user,Date from,Date to){




		return dataStore.getTimesheetEntriesSummary(getvalidString(user),from,to,getProjectLeaveCodes())
	}



	public def getProjectHoursReport(String project,Date from,Date to){




		return dataStore.getProjectHoursReport(getvalidString(project),from,to)
	}


	public def getProjectEmployeeReport(String project,Date from,Date to){




		return dataStore.getProjectEmployeeReport(getvalidString(project),from,to)
	}
	public ArrayList<TimeEntry> getTimesheetEntries(String user,Date from,Date to){



		return dataStore.getTimesheetEntries(getvalidString(user),from,to,getProjectLeaveCodes())
	}

	public ArrayList<TimeEntry> getTimesheetEntries(Date from,Date to){

		return getTimesheetEntries(null,from,to)
	}
	public ArrayList<TimeEntry> getTimesheetEntries(String user){

		return getTimesheetEntries(user,null,null)
	}
}

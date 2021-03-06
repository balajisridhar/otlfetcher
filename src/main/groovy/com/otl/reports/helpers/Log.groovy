package com.otl.reports.helpers

public  class Log {

	static def info(def obj){

		//Thread.start {
		printmsg("OTLREPORTFETCHER INFO - ${new Date()} : " + obj?.toString())
		//}
	}

	static def debug(def obj){

		//Thread.start {
		printmsg("OTLREPORTFETCHER Debug - ${new Date()} : " + obj?.toString())
		//}
	}

	static def error(def obj){

		//Thread.start {
		printmsg("OTLREPORTFETCHER Error - ${new Date()} : " + obj?.toString())
		//}
	}



	private static def printmsg(final Object msg){

		println(msg?.toString());
	}
}

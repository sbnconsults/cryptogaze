/*
 *  Name: MyScheduleExecutor.java
 *  Description: To scheduler and executer the updater task
 *
 */
package com.idealtechlabs.cryptogaze;

import java.text.DateFormat;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
 
public class MyScheduleExecutor extends ScheduledThreadPoolExecutor {
	final static DateFormat fmt = DateFormat.getTimeInstance(DateFormat.LONG);

	public MyScheduleExecutor(int corePoolSize) {
		super(corePoolSize);
		// TODO Auto-generated constructor stub
	}
	   
	public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
	}

	private Runnable wrapRunnable(Runnable command) {
		return new LogOnExceptionRunnable(command);
	}

	private class LogOnExceptionRunnable implements Runnable {
		private Runnable theRunnable;

		public LogOnExceptionRunnable(Runnable theRunnable) {
			super();
			this.theRunnable = theRunnable;
		}

		@Override
		public void run() {
			try {
				theRunnable.run();
			} catch (Exception e) {
				// LOG IT HERE!!!
				System.err.println("error in executing: " + theRunnable + ". It will no longer be run!");
				e.printStackTrace();

				// and re throw it so that the Executor also gets this error so that it can do what it would
				// usually do
				throw new RuntimeException(e);
			}
		}
	}
}


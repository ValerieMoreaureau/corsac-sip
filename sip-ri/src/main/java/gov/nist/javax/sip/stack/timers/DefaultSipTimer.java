/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
*
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*
* .
*
*/
package gov.nist.javax.sip.stack.timers;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SipStackImpl;

/**
 * Default SIP Timer implementation based on java.util.Timer 
 * 
 * @author jean.deruelle@gmail.com
 *
 */
public class DefaultSipTimer extends Timer implements SipTimer {
	private static StackLogger logger = CommonLogger.getLogger(DefaultSipTimer.class);

	protected AtomicBoolean started = new AtomicBoolean(false);
	protected SipStackImpl sipStackImpl;
	
        public DefaultSipTimer() {
            super("DefaultSipTimerThread");
        }
        
	private class DefaultTimerTask extends TimerTask {
		private SIPTimerTask task;

		public DefaultTimerTask(SIPTimerTask task) {
			this.task= task;
			((SIPStackTimerTask)task).setSipTimerTask(this);
		}
		
		public void run() {
			 try {
				 // task can be null if it has been cancelled
				 if(task != null) {
					 Thread.currentThread().setName(((SIPStackTimerTask)task).getTaskName());
					 task.runTask();					 
				 }
	        } catch (Exception e) {
	            logger.logError("SIP stack timer task failed due to exception:", e);
	            e.printStackTrace();
	        }
		}
		
		public boolean cancel() {
			if(task != null) {
				((SIPStackTimerTask)task).cleanUpBeforeCancel();
				task = null;
			}
			return super.cancel();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#schedule(gov.nist.javax.sip.stack.SIPStackTimerTask, long)
	 */
	@Override
	public boolean schedule(SIPStackTimerTask task, long delay) {
		if(!started.get()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		super.schedule(new DefaultTimerTask(task), delay);
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#scheduleWithFixedDelay(gov.nist.javax.sip.stack.SIPStackTimerTask, long, long)
	 */
	@Override
	public boolean scheduleWithFixedDelay(SIPStackTimerTask task, long delay,
			long period) {
		if(!started.get()) {
			throw new IllegalStateException("The SIP Stack Timer has been stopped, no new tasks can be scheduled !");
		}
		super.schedule(new DefaultTimerTask(task), delay, period);
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#cancel(gov.nist.javax.sip.stack.SIPStackTimerTask)
	 */
	@Override
	public boolean cancel(SIPStackTimerTask task) {
		if(task != null & ((SIPStackTimerTask)task).getSipTimerTask() != null)
			return ((TimerTask)((SIPStackTimerTask)task).getSipTimerTask()).cancel();
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#start(gov.nist.javax.sip.SipStackImpl)
	 */
	public void start(SipStackImpl sipStack) {
		sipStackImpl= sipStack;
		// don't need the properties so nothing to see here
		started.set(true);
		if(logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
			logger.logInfo("the sip stack timer " + this.getClass().getName() + " has been started");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#stop()
	 */
	public void stop() {
		started.set(false);
		cancel();		
		logger.logStackTrace(StackLogger.TRACE_DEBUG);
		if(logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
			logger.logInfo("the sip stack timer " + this.getClass().getName() + " has been stopped");
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see gov.nist.javax.sip.stack.timers.SipTimer#isStarted()
	 */
	public boolean isStarted() {
		return started.get();
	}


}

package com.sodavision.pylonandroid;


import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** Thread-safe command processor, like the Java ExecutorService
 **/
public class CommandProcessor extends Thread{
    final Lock                  m_ClassLock = new ReentrantLock();
    final Condition             m_DataAvailable = m_ClassLock.newCondition();
    final int                   m_MaxCapacity;

    ArrayDeque<com.sodavision.pylonandroid.ICommand>        m_Data = new ArrayDeque<>();
    boolean                     m_StopProcessing = false;
    boolean                     m_StopWhenEmpty = false;
    /** constructor with max capacity
     * @param maxCapacity; maximum capacity for queue, if append exceeds. oldest command is discarded
     * */
    CommandProcessor(int maxCapacity) {
        if(maxCapacity <=0 )
            throw new IllegalArgumentException();
        m_MaxCapacity = maxCapacity;
    }

    /** Cancel any pending work item
     * and wait for completion of the current executed one.
     **/
    public void stopProcessing(){
        // clear all items and wake up the worker to exit
        m_ClassLock.lock();
        try {
            m_StopProcessing = true;
            m_Data.clear();
            m_DataAvailable.signalAll();
        }
        finally {
            m_ClassLock.unlock();
        }

        // wait for exit of the worker thread
        try{
            this.join();
        }catch (InterruptedException ignored) {
        }
    }
    
    /** Stop processing when all current work is done.
     * The worker thread are exit after this call.
     **/
    public void stopWhenEmpty(){
        m_ClassLock.lock();
        try {
            m_StopWhenEmpty = true;
            if( m_Data.isEmpty() ) {
                m_StopProcessing = true;
            }
            m_DataAvailable.signalAll();
        }
        finally {
            m_ClassLock.unlock();
        }

        // wait for exit of the worker thread
        try{
            this.join();
        }catch (InterruptedException ignored) {
        }
    }
    
    /** Append a command to the processing queue.
     * @param c : command to append, if maximum capacity is exceeded the oldest command is discarded
     **/
    public void append(SaveImageCommand c) {
        m_ClassLock.lock();
        try {
            if( m_Data.size() >= m_MaxCapacity) {
                m_Data.pollFirst().discard();
            }
            m_Data.addLast( c );
            m_DataAvailable.signal();
        }
        finally {
            m_ClassLock.unlock();
        }
    }
    
    /** Thread worker function.
     *  Cancel with stopProcesing.
     *  Add work with append.
     **/
    public void run() {
        m_ClassLock.lock();
        try {
            while( !m_StopProcessing ) {

                // no new Data, wait for new one or for an abort
                if(m_Data.isEmpty()) {
                    m_DataAvailable.await();
                }
                if(m_StopProcessing)
                    break;

                // we wake up with new data, execute it
                if ( !m_Data.isEmpty() ) {
                    com.sodavision.pylonandroid.ICommand currentJob = m_Data.removeFirst();

                    // unblock while processing
                    m_ClassLock.unlock();
                    currentJob.execute();
                    m_ClassLock.lock();
                }

                // last item done, should we exit?
                if( m_Data.isEmpty() && m_StopWhenEmpty) {
                    break;
                }
            }
        }
        catch(InterruptedException ignored) {
        }
        finally {
            m_ClassLock.unlock();
        }
    }
}

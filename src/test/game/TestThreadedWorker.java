package test.game;

import java.util.Random;

import nidefawl.qubes.util.IThreadedWork;
import nidefawl.qubes.util.ThreadedWorker;

public class TestThreadedWorker implements IThreadedWork {
	final static int WORK_LOAD = 4*1024*1024;
	final static boolean HIGH_COMPUTATIONAL_LOAD = true;
	final static float[] inputData = new float[WORK_LOAD];
	final static Random r = new Random(0xC0FFEE);
	static {
		for (int i = 0; i < inputData.length; i++) {
			inputData[i] = r.nextFloat()*128;
		}
	}
	static class WorkData {
		float[] out;
		public WorkData() {
			out = new float[WORK_LOAD];
		}
		public void process(int threadId, int maxThreads) {
			int perThread = (int) Math.ceil(inputData.length/(float)maxThreads);
			int start = threadId*perThread;
			int end = start+perThread;
			if (threadId == maxThreads-1) {
				end = inputData.length;
			}
//			System.out.println("Thread "+threadId+" is working on "+start+"-"+end);
			
			for (int i = start; i < end; i++) {
				out[i] = (float) Math.sqrt(inputData[i]); 
				out[i] = (float) Math.sqrt(32+out[i]);
				if (!HIGH_COMPUTATIONAL_LOAD) {
					out[i] = (float) Math.sqrt(32+out[i]);
					out[i] = (float) Math.sqrt(32+out[i]);
					out[i] = (float) Math.sqrt(32+out[i]); 
					
				}
			}
		}
		public boolean isEqual(WorkData w) {
			for (int i = 0; i < out.length; i++) {
				if (out[i] != w.out[i]) {
//					System.out.println("diff at "+i+" "+out[i]+" != "+w.out[i]);
					return false;
				}
			}
			return true;
		}
	}
	public static void main(String[] args) {
		new TestThreadedWorker().doStuff();
	}

	private WorkData data1;
	private WorkData singleThreadedResult;
	private ThreadedWorker worker;
	public TestThreadedWorker() {
		worker = new ThreadedWorker(6);
	}
	void doStuff() {
		worker.init();
		singleThreadedResult = runSingleThreaded();
		WorkData d2 = runMultiThreaded();

//		worker.stopThread();
//		worker.init();
		for (int i = 0; i < 4; i++)
			runSingleThreaded();
		for (int i = 0; i < 4; i++)
			runMultiThreaded();
//
		{

			long nStart = System.nanoTime();
			for (int i = 0; i < 32; i++)
				runSingleThreaded();
			long nEnd = System.nanoTime();
			long nTook = (nEnd - nStart)/1000L;
			System.out.println("nonthreaded took "+(nTook));
		}
		{

			long nStart = System.nanoTime();
			for (int i = 0; i < 32; i++)
				runMultiThreaded();
			long nEnd = System.nanoTime();
			long nTook = (nEnd - nStart)/1000L;
			System.out.println("threaded took "+(nTook));
		}
		for (int i = 0; i < 1555; i++) {
			runMultiThreaded();
			if (i%1000==0) {
				System.out.println("at "+i);
			}
		}

		System.out.println("end");
	}
	private WorkData runSingleThreaded() {
		data1 = new WorkData();
		data1.process(0, 1);
		return data1;
	}
	private WorkData runMultiThreaded() {
		data1 = new WorkData();
		worker.work(this);
		if (!singleThreadedResult.isEqual(data1)) {
			throw new RuntimeException("INVALID RESULT");
		}
		return data1;
	}
	@Override
	public void fromThread(int threadId, int maxThreads) {
		data1.process(threadId, maxThreads);
	}
}

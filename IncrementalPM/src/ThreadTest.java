import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadTest {

	static int THREAD_NO = 50;
	static int totalTasks=50;

	public static void main(String[] args) throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_NO);
		List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
		TestClass testClass = new TestClass();
		while (true) {
			for(int i=0;i<totalTasks;i++) {
				System.out.println("Main: Adding Task "+i);
				futures.add(executorService.submit(testClass));
				System.out.println("Main: Active Threads-"+Thread.activeCount());
			}
			executorService.awaitTermination(1000000, TimeUnit.MINUTES);
			System.out.println("All tasks finished");
		}
	}
}

class TestClass implements Callable<Integer>{
	private ThreadLocal<Integer> id;
	int i;
	ReentrantLock l;
	
	public TestClass() {
		id = new ThreadLocal<Integer>();
		l=new ReentrantLock();
		i=0;
	}
	
	public Integer call() throws Exception {
		l.lock();
		i+=1;
		id.set(i);
		l.unlock();
		System.out.println("Thread "+this.id.get()+": Starting");
		Thread.sleep(100*this.id.get());
		System.out.println(" Thread "+this.id.get()+": Shutting Down");
		return this.i;
	}
	
}

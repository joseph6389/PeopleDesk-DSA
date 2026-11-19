import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public final class TestRunner {
 public static void main(String[] args) {
  LauncherDiscoveryRequest request=LauncherDiscoveryRequestBuilder.request().selectors(selectPackage("edu.college.employee")).build();
  SummaryGeneratingListener listener=new SummaryGeneratingListener();
  Launcher launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(request);
  var summary=listener.getSummary();summary.printTo(new java.io.PrintWriter(System.out));
  if(!summary.getFailures().isEmpty()){summary.printFailuresTo(new java.io.PrintWriter(System.out));System.exit(1);}
 }
}



import androidx.fragment.app.Fragment;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.tnote.Editor.EditorFragment;
import com.example.tnote.R;
import com.example.tnote.testActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(AndroidJUnit4.class)
public class EditorFragmentTest {

    @Rule
    public ActivityScenarioRule<testActivity> activityRule = new ActivityScenarioRule<>(testActivity.class);

    @Test
    public void testFragmentLoads() {
        // 创建 EditorFragment 的实例并添加到 Activity
        activityRule.getScenario().onActivity(activity -> {

            Fragment fragment = new EditorFragment();
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.left_pane, fragment) // 确保替换到正确的容器
                    .commitNow();
        });
        onView(withId(R.id.left_pane)).check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                if (view instanceof EditText) {
                    Log.d("EditorView", "Width: " + view.getWidth() + ", Height: " + view.getHeight());
                }
            }
        });
        // 等待 Fragment 加载完成
        onView(withId(R.id.left_pane)).check(matches(isDisplayed()));

        // 验证编辑器是否显示
        onView(withId(R.id.left_pane)).check(matches(isDisplayed()));

        // 你可以添加一个延迟或等待
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public File createTempFileFromInputStream(InputStream inputStream, String fileName) throws IOException {
        File tempFile = File.createTempFile(fileName, null);
        tempFile.deleteOnExit();
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
}

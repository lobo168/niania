// Generated code from Butter Knife. Do not modify!
package mgr.niania;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.github.niqdev.mjpeg.MjpegView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class ListenActivity_ViewBinding<T extends ListenActivity> implements Unbinder {
  protected T target;

  @UiThread
  public ListenActivity_ViewBinding(T target, View source) {
    this.target = target;

    target.mjpegView = Utils.findRequiredViewAsType(source, R.id.mjpegViewDefault, "field 'mjpegView'", MjpegView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    T target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");

    target.mjpegView = null;

    this.target = null;
  }
}

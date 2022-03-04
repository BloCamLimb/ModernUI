/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.lifecycle;

import icyllis.modernui.annotation.UiThread;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * ViewModel is a class that is responsible for preparing and managing the data for
 * a {@link icyllis.modernui.fragment.Fragment Fragment}.
 * It also handles the communication of the Fragment with the rest of the application
 * (e.g. calling the business logic classes).
 * <p>
 * A ViewModel is always created in association with a scope (a fragment) and will
 * be retained as long as the scope is alive.
 * <p>
 * In other words, this means that a ViewModel will not be destroyed if its owner is
 * destroyed for a configuration change (e.g. density / view scale). The new owner
 * instance just re-connects to the existing model.
 * <p>
 * The purpose of the ViewModel is to acquire and keep the information that is necessary
 * for a Fragment. The Fragment should be able to observe changes in the ViewModel.
 * ViewModels usually expose this information via {@link LiveData} or Two-Way Data
 * Binding. You can also use any observability construct from your favorite framework.
 * <p>
 * ViewModel's only responsibility is to manage the data for the UI. It <b>should never</b>
 * access your view hierarchy or hold a reference back to the Fragment.
 * <p>
 * ViewModels can be used as a communication layer between different Fragments.
 * Each Fragment can acquire the ViewModel using the same key. This allows
 * communication between Fragments in a de-coupled fashion such that they never need to
 * talk to the other Fragment directly.
 * <pre>{@code
 * public class MyFragment extends Fragment {
 *     @Override
 *     public void onCreate(@Nullable DataSet savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         final UserModel userModel = new ViewModelProvider(this).get(UserModel.class);
 *         userModel.getUser().observe(this, new Observer<User>() {
 *             @Override
 *             public void onChanged(@Nullable User data) {
 *                 // update ui.
 *             }
 *         });
 *         findViewById(R.id.button)
 *             .setOnClickListener(v -> userModel.doAction());
 *     }
 * }
 * }</pre>
 * ViewModel would be:
 * <pre>{@code
 * public class UserModel extends ViewModel {
 *     private final MutableLiveData<User> userLiveData = new MutableLiveDat<>();
 *
 *     public LiveData<User> getUser() {
 *         return userLiveData;
 *     }
 *
 *     public UserModel() {
 *         // trigger user load.
 *     }
 *
 *     void doAction() {
 *         // depending on the action, do necessary business logic calls and update the
 *         // userLiveData.
 *     }
 * }
 * }</pre>
 */
public abstract class ViewModel {

    // this abstract class is preserved for future internal changes
    static final Marker MARKER = MarkerManager.getMarker("ViewModel");

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     * <p>
     * It is useful when ViewModel observes some data, and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    protected void onCleared() {
    }

    @UiThread
    final void clear() {
        onCleared();
    }
}

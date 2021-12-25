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

package icyllis.modernui.fragment;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

/**
 * Interface used to control the instantiation of {@link Fragment} instances.
 * Implementations can be registered with a {@link FragmentManager} via
 * {@link FragmentManager#setFragmentFactory(FragmentFactory)}.
 *
 * @see FragmentManager#setFragmentFactory(FragmentFactory)
 */
@ApiStatus.Experimental
public class FragmentFactory {

    /**
     * Parse a Fragment Class from the given class name. The resulting Class is kept in a global
     * cache, bypassing the {@link Class#forName(String)} calls when passed the same
     * class name again.
     *
     * @param classLoader The classloader to use for loading the Class
     * @param className   The class name of the fragment to parse.
     * @return Returns the parsed Fragment Class
     * @throws RuntimeException If there is a failure in parsing
     *                          the given fragment class.  This is a runtime exception; it is not
     *                          normally expected to happen.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static Class<? extends Fragment> loadFragmentClass(@Nonnull ClassLoader classLoader,
                                                              @Nonnull String className) {
        try {
            //Class.forName(className, false, classLoader);
            Class<?> clazz = classLoader.loadClass(className);
            return (Class<? extends Fragment>) clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to instantiate fragment " + className
                    + ": make sure class name exists", e);
        } catch (ClassCastException e) {
            throw new RuntimeException("Unable to instantiate fragment " + className
                    + ": make sure class is a valid subclass of Fragment", e);
        }
    }

    /**
     * Create a new instance of a Fragment with the given class name. This uses
     * {@link #loadFragmentClass(ClassLoader, String)} and then {@link #instantiate(Class)}.
     *
     * @param classLoader The classloader to use for instantiation
     * @param className   The class name of the fragment to instantiate.
     * @return Returns a new fragment instance.
     * @throws RuntimeException If there is a failure in instantiating
     *                          the given fragment class.  This is a runtime exception; it is not
     *                          normally expected to happen.
     */
    @Nonnull
    public Fragment instantiate(@Nonnull ClassLoader classLoader, @Nonnull String className) {
        return instantiate(loadFragmentClass(classLoader, className));
    }

    /**
     * Create a new instance of a Fragment with the given class name. This the empty
     * constructor of the resulting Class by default.
     *
     * @param clazz The loaded class of the fragment to instantiate.
     * @return Returns a new fragment instance.
     * @throws RuntimeException If there is a failure in instantiating
     *                          the given fragment class.  This is a runtime exception; it is not
     *                          normally expected to happen.
     */
    @Nonnull
    public Fragment instantiate(@Nonnull Class<? extends Fragment> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate fragment " + clazz
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to instantiate fragment " + clazz
                    + ": could not find Fragment constructor", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to instantiate fragment " + clazz
                    + ": calling Fragment constructor caused an exception", e);
        }
    }
}

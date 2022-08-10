/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine.shading;

import icyllis.arcui.core.Matrix3;
import icyllis.arcui.core.Matrix4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Manages the resources used by a shader program.
 * <p>
 * The resources are objects the program uses to communicate with the application code.
 */
public interface ProgramDataManager {

    /**
     * Marks an integer as an opaque handle to a uniform resource.
     */
    @Retention(RetentionPolicy.SOURCE)
    @interface UniformHandle {
    }

    /**
     * Specifies the value of an int, uint or bool uniform variable for the current program object.
     */
    void set1i(@UniformHandle int u, int v0);

    /**
     * Specifies the value of a single int, uint or bool uniform variable or an int, uint or bool
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set1iv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set1iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set1iv(@UniformHandle int u, int offset, int count, int[] value);

    /**
     * Specifies the value of a float uniform variable for the current program object.
     */
    void set1f(@UniformHandle int u, float v0);

    /**
     * Specifies the value of a single float uniform variable or a float uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set1fv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set1fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set1fv(@UniformHandle int u, int offset, int count, float[] value);

    /**
     * Specifies the value of an ivec2, uvec2 or bvec2 uniform variable for the current program object.
     */
    void set2i(@UniformHandle int u, int v0, int v1);

    /**
     * Specifies the value of a single ivec2, uvec2 or bvec2  uniform variable or an ivec2, uvec2 or bvec2
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set2iv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set2iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set2iv(@UniformHandle int u, int offset, int count, int[] value);

    /**
     * Specifies the value of a vec2 uniform variable for the current program object.
     */
    void set2f(@UniformHandle int u, float v0, float v1);

    /**
     * Specifies the value of a single vec2 uniform variable or a vec2 uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set2fv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set2fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set2fv(@UniformHandle int u, int offset, int count, float[] value);

    /**
     * Specifies the value of an ivec3, uvec3 or bvec3 uniform variable for the current program object.
     */
    void set3i(@UniformHandle int u, int v0, int v1, int v2);

    /**
     * Specifies the value of a single ivec3, uvec3 or bvec3 uniform variable or an ivec3, uvec3 or bvec3
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set3iv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set3iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set3iv(@UniformHandle int u, int offset, int count, int[] value);

    /**
     * Specifies the value of a vec3 uniform variable for the current program object.
     */
    void set3f(@UniformHandle int u, float v0, float v1, float v2);

    /**
     * Specifies the value of a single vec3 uniform variable or a vec3 uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set3fv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set3fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set3fv(@UniformHandle int u, int offset, int count, float[] value);

    /**
     * Specifies the value of an ivec4, uvec4 or bvec4 uniform variable for the current program object.
     */
    void set4i(@UniformHandle int u, int v0, int v1, int v2, int v3);

    /**
     * Specifies the value of a single ivec4, uvec4 or bvec4 uniform variable or an ivec4, uvec4 or bvec4
     * uniform variable array for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set4iv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set4iv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set4iv(@UniformHandle int u, int offset, int count, int[] value);

    /**
     * Specifies the value of a vec4 uniform variable for the current program object.
     */
    void set4f(@UniformHandle int u, float v0, float v1, float v2, float v3);

    /**
     * Specifies the value of a single vec4 uniform variable or a vec4 uniform variable array
     * for the current program object.
     *
     * @param count the number of elements that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array, and 1 or more if it is an array
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void set4fv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #set4fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void set4fv(@UniformHandle int u, int offset, int count, float[] value);

    /**
     * Specifies the value of a single mat2 uniform variable or a mat2 uniform variable array
     * for the current program object. Matrices are column-major.
     *
     * @param count the number of matrices that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array of matrices, and 1 or more if it is an array of matrices.
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void setMatrix2fv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #setMatrix2fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void setMatrix2fv(@UniformHandle int u, int offset, int count, float[] value);

    /**
     * Specifies the value of a single mat3 uniform variable or a mat3 uniform variable array
     * for the current program object. Matrices are column-major.
     *
     * @param count the number of matrices that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array of matrices, and 1 or more if it is an array of matrices.
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void setMatrix3fv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #setMatrix3fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void setMatrix3fv(@UniformHandle int u, int offset, int count, float[] value);

    /**
     * Specifies the value of a single mat4 uniform variable or a mat4 uniform variable array
     * for the current program object. Matrices are column-major.
     *
     * @param count the number of matrices that are to be modified. This should be 1 if the targeted uniform
     *              variable is not an array of matrices, and 1 or more if it is an array of matrices.
     * @param value a pointer to an array of {@code count} values that will be used to update the specified uniform
     *              variable
     */
    void setMatrix4fv(@UniformHandle int u, int count, long value);

    /**
     * Array version of {@link #setMatrix4fv(int, int, long)}.
     *
     * @param offset the start index in the array
     */
    void setMatrix4fv(@UniformHandle int u, int offset, int count, float[] value);

    /**
     * Convenience method for uploading a Matrix3 to a 3x3 matrix uniform.
     */
    void setMatrix3f(@UniformHandle int u, Matrix3 matrix);

    /**
     * Convenience method for uploading a Matrix4 to a 4x4 matrix uniform.
     */
    void setMatrix4f(@UniformHandle int u, Matrix4 matrix);
}

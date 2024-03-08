/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler;

/**
 * Holds the options for compiling a shader executable.
 * This is ignored when pre-parsing a module unit (include file).
 */
public class CompileOptions {

    /**
     * Whether to do preprocessing? If you have done preprocessing, you may set
     * this to false.
     * <p>
     * Note: Our preprocessor is not available for module units.
     *
     * @see Parser#preprocess()
     */
    public boolean mPreprocess = true;

    /**
     * Function with this name will be considered as the entry point of
     * a shader executable.
     */
    public String mEntryPointName = "main";

    /**
     * No relax precision when generating SPIR-V.
     * No mediump precision when generating GLSL.
     */
    public boolean mForceHighPrecision = false;

    /**
     * Allow precision qualifiers (mediump, highp) when generating GLSL.
     * This can be used for GLSL Vulkan and GLSL ES.
     */
    public boolean mUsePrecisionQualifiers = false;

    /**
     * Force no short-circuit operators when generating code.
     * <p>
     * For '&&', '||' and '?:', always evaluate both sides.
     * No branching is usually more performant.
     * <p>
     * Use with caution, may have side effects.
     */
    public boolean mNoShortCircuit = false;

    /**
     * Try to minify symbol names (variables, functions, structure members).
     * <p>
     * At AST level, all private symbols will be renamed and minified.
     * <p>
     * For GLSL, minify names that have no semantic impact. This depends on
     * the target GLSL version. For example, interface blocks with binding
     * and vertex attributes with location will have their names minified.
     * <p>
     * For SPIR-V, the generator will not emit any OpName or OpMemberName
     * instructions, so that all names are removed.
     * <p>
     * If this is false, the generator will output mangled function names,
     * except for the entry point.
     */
    public boolean mMinifyNames = true;

    /**
     * Try to minify code when generating GLSL. Whitespaces and unnecessary
     * characters will be removed. Otherwise, the generated code may be pretty
     * printed with indentation.
     * <p>
     * This option may be useful when generating SPIR-V. This option does not
     * mean eliminating unused variables and functions, see {@link #mOptimizationLevel}.
     */
    public boolean mMinifyCode = true;

    /**
     * Optimization level. (0-2)
     * <p>
     * Even if this is 0, the compiler can still do some optimizations.
     */
    public int mOptimizationLevel = 1;
}

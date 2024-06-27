/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite;

// ShaderNodes organize snippets into an effect tree, and provide random access to the dynamically
// bound child snippets. Each node has a fixed number of children defined by its code ID
// (either a BuiltInCodeSnippetID or a runtime effect's assigned ID). All children are non-null.
// A ShaderNode tree represents a decompressed PaintParamsKey.
public class FragmentNode {

    public static final FragmentNode[] NO_CHILDREN = new FragmentNode[0];

    private final FragmentStage mStage;
    private final FragmentNode[] mChildren;

    private final int mCodeID;
    private final int mStageIndex;

    private final int mRequirementFlags;

    public FragmentNode(FragmentStage stage, FragmentNode[] children, int codeID, int stageIndex) {
        assert stage.mNumChildren == children.length;
        mStage = stage;
        mChildren = children;
        mCodeID = codeID;
        mStageIndex = stageIndex;
        int requirementFlags = stage.mRequirementFlags;
        for (FragmentNode child : children) {
            requirementFlags |= child.mRequirementFlags;
        }
        mRequirementFlags = requirementFlags;
    }

    public FragmentStage stage() {
        return mStage;
    }

    public int codeID() {
        return mCodeID;
    }

    public int stageIndex() {
        return mStageIndex;
    }

    public int numChildren() {
        return mStage.mNumChildren;
    }

    public FragmentNode childAt(int index) {
        return mChildren[index];
    }

    public FragmentNode[] children() {
        return mChildren;
    }

    public int requirementFlags() {
        return mRequirementFlags;
    }
}

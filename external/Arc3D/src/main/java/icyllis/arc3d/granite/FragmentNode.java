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

/**
 * FragmentNodes organize stages into an effect tree, and provide random access to the dynamically
 * bound child stages. Each node has a fixed number of children defined by its stage ID
 * (either a BuiltinStageID or a custom effect's assigned ID). All children are non-null.
 * A FragmentNode tree represents a decompressed PaintParamsKey.
 */
public class FragmentNode {

    public static final FragmentNode[] NO_CHILDREN = new FragmentNode[0];

    private final FragmentStage mStage;
    private final FragmentNode[] mChildren;

    private final int mStageID;
    private final int mStageIndex;

    private final int mRequirementFlags;

    public FragmentNode(FragmentStage stage, FragmentNode[] children, int stageID, int stageIndex) {
        assert stage.mNumChildren == children.length;
        mStage = stage;
        mChildren = children;
        mStageID = stageID;
        mStageIndex = stageIndex;
        boolean isCompose = stageID == FragmentStage.kCompose_BuiltinStageID ||
                stageID == FragmentStage.kBlend_BuiltinStageID;
        int requirementFlags = stage.mRequirementFlags;
        for (FragmentNode child : children) {
            int mask = 0;
            if (stageID >= FragmentStage.kBuiltinStageIDCount ||
                    (isCompose && child == children[children.length - 1])) {
                // Only mask off the variable arguments; any special behaviors always propagate.
                mask = FragmentStage.kLocalCoords_ReqFlag |
                        FragmentStage.kPriorStageOutput_ReqFlag |
                        FragmentStage.kBlenderDstColor_ReqFlag;
            }
            requirementFlags |= child.mRequirementFlags & ~mask;
        }
        mRequirementFlags = requirementFlags;
    }

    public FragmentStage stage() {
        return mStage;
    }

    public int stageID() {
        return mStageID;
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

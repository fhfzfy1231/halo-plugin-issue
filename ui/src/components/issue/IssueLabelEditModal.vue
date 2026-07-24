<script lang="ts" setup>
import { VModal, VButton, VSpace, Toast } from "@halo-dev/components";
import { computed, nextTick, ref, toRaw, watchEffect } from "vue";
import type { IssueLabel } from "@/api/generated";
import cloneDeep from "lodash.clonedeep";
import {
  issueLabelApiClient,
  consoleIssueLabelApiClient,
} from "@/api";
import { submitForm } from "@formkit/core";
const modalTitle = ref("新增 issue 标签");
const saving = ref<boolean>(false);
const props = withDefaults(
  defineProps<{
    visible: boolean;
    issueLabel?: IssueLabel | undefined;
  }>(),
  {
    visible: false,
    issueLabel: undefined,
  },
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close", value: boolean): void;
  (event: "save", issueLabel: IssueLabel): void;
  (event: "update", issueLabel: IssueLabel): void;
}>();

const initIssueLabel: IssueLabel = {
  kind: "IssueLabel",
  apiVersion: "issue.foxbridge.team/v1alpha1",
  metadata: {
    generateName: "label-",
    name: "",
  },
  spec: {
    labelName: "",
    description: "",
    color: "#71C8A3",
    slug: "",
    scope: "GLOBAL",
  },
};

const formState = ref<IssueLabel>(cloneDeep(initIssueLabel));

watchEffect(() => {
  if (props.issueLabel) {
    formState.value = cloneDeep(props.issueLabel);
    modalTitle.value = "编辑 issue 标签";
  }
});

const isUpdateMode = computed(
  () => !!formState.value.metadata.creationTimestamp,
);
const isEditorEmpty = ref<boolean>(true);

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close", false);
    handleReset();
  }
};

const annotationsFormRef = ref();
const onSubmit = async () => {
  try {
    saving.value = true;
    annotationsFormRef.value?.handleSubmit();
    await nextTick();

    const {
      customAnnotations,
      annotations,
      customFormInvalid,
      specFormInvalid,
    } = annotationsFormRef.value || {};
    if (customFormInvalid || specFormInvalid) {
      return;
    }
    formState.value.metadata.annotations = {
      ...annotations,
      ...customAnnotations,
    };
    formState.value.spec.scope = "GLOBAL";
    formState.value.spec.subjectType = undefined;
    formState.value.spec.subjectName = undefined;

    if (isUpdateMode.value) {
      await handleUpdate();
      emit("update", formState.value);
    } else {
      await handleSave(formState.value);
    }
    handleReset();
  } catch (error) {
    console.error(error);
  } finally {
    saving.value = false;
  }
  onVisibleChange(false);
  formState.value = cloneDeep(initIssueLabel);
};
const handleUpdate = async () => {
  const res = await issueLabelApiClient.issueLabel.updateIssueLabel({
    name: formState.value.metadata.name,
    issueLabel: formState.value,
  });
  if (res.status == 200) {
    Toast.success("更新成功!");
  }
};

// 新增 issue
const handleSave = async (issueLabel: IssueLabel) => {
  const { data } = await consoleIssueLabelApiClient.issueLabel.createIssueLabel(
    {
      issueLabel: issueLabel,
    },
  );
  emit("save", data);
  Toast.success("成功新增标签");
};
const handleReset = () => {
  formState.value = toRaw(cloneDeep(initIssueLabel));
  isEditorEmpty.value = true;
};
</script>
<template>
  <VModal
    :title="modalTitle"
    :visible="visible"
    :width="720"
    @update:visible="onVisibleChange"
  >
    <template #actions>
      <slot name="append-actions" />
    </template>
    <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
      <div class=":uno: mt-2.5 px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> Issue详情 </span>
        </div>
      </div>
      <!-- 提交表单  -->
      <div class=":uno: divide-gray-25 mt-5 px-3 md:col-span-3 md:mt-3 divide-y">
        <FormKit
          id="issue-label"
          v-model="formState.spec"
          type="form"
          name="issue-label"
          :config="{ validationVisibility: 'submit' }"
          @submit="onSubmit"
        >
          <FormKit
            type="text"
            label="标签名称"
            name="labelName"
            validation="required"
            modifiers="trim"
          />
          <FormKit name="color" label="标签颜色" type="color"></FormKit>
          <FormKit
            type="textarea"
            rows="3"
            name="description"
            label="标签描述"
          />
        </FormKit>
      </div>
    </div>
    <div class="py-5">
      <div class="border-t border-gray-200"></div>
    </div>
    <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
      <div class=":uno: px-3 md:col-span-1">
        <div class="sticky top-0">
          <span class="text-base text-gray-900 font-medium"> 元数据 </span>
        </div>
      </div>
      <div
        class=":uno: divide-gray-25 mt-5 w-full px-3 md:col-span-3 md:mt-0 divide-y"
      >
        <AnnotationsForm
          v-if="visible"
          :key="formState.metadata.name"
          ref="annotationsFormRef"
          :value="formState.metadata.annotations"
          kind="IssueLabel"
          group="issue.foxbridge.team"
        />
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton
          :loading="saving"
          type="secondary"
          @click="submitForm('issue-label')"
        >
          提交
        </VButton>
        <VButton @click="onVisibleChange(false)"> 取消 </VButton>
      </VSpace>
    </template>
  </VModal>
</template>

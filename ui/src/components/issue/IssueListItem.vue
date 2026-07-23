<script lang="ts" setup>
import { formatDatetime } from "@/utils/date";
import {
  Dialog,
  Toast,
  VAlert,
  VAvatar,
  VButton,
  VDropdownDivider,
  VDropdownItem,
  VEmpty,
  VEntity,
  VEntityContainer,
  VEntityField,
  VLoading,
  VModal,
  VSpace,
  VStatusDot,
} from "@halo-dev/components";
import { computed, inject, provide, type Ref, ref, watch } from "vue";
import { useQueryClient } from "@tanstack/vue-query";
import type { Issue, ListedIssue, ListedIssueComment } from "@/api/generated";
import IssueCommentItem from "@/components/issue/IssueCommentItem.vue";
import { consoleIssueApiClient, issueApiClient } from "@/api";
import { useIssueCommentListFetch } from "@/composables/use-consoleIssue";

type LabelOption = {
  label: string;
  value?: string;
  color?: string;
};

const props = withDefaults(
  defineProps<{
    issue: ListedIssue;
    labelOptions?: LabelOption[];
    isSelected?: boolean;
  }>(),
  { labelOptions: () => [], isSelected: false },
);

const emit = defineEmits<{
  (event: "update", value: Issue): void;
}>();
const queryClient = useQueryClient();
const detailVisible = ref(false);
const closeVisible = ref(false);
const closeComment = ref("");
const closing = ref(false);
const savingLabels = ref(false);
const selectedLabels = ref<string[]>([...(props.issue.issue.spec.labels || [])]);
const selectedIssueNames = inject<Ref<string[]>>(
  "selectedIssueMessageNames",
  ref([]),
);

watch(
  () => props.issue.issue.spec.labels,
  (labels) => (selectedLabels.value = [...(labels || [])]),
);

const { issueComments, refetch: refetchComments, isLoading } =
  useIssueCommentListFetch(props.issue.issue.metadata.name, detailVisible);
const hoveredReply = ref<ListedIssueComment>();
provide<Ref<ListedIssueComment | undefined>>("hoveredIssueComment", hoveredReply);

const statusText = computed(() => {
  const state = props.issue.issue.status?.state;
  return state === "AWAIT" ? "待处理" : state === "PROGRESS" ? "进行中" : "已关闭";
});
const statusState = computed(() =>
  props.issue.issue.status?.state === "AWAIT"
    ? "warning"
    : props.issue.issue.status?.state === "PROGRESS"
      ? "default"
      : "success",
);
const labelOption = (name: string) =>
  props.labelOptions.find((option) => option.value === name);

async function changeLabel(labelName: string, checked: boolean) {
  const previous = [...selectedLabels.value];
  const next = checked
    ? Array.from(new Set([...previous, labelName]))
    : previous.filter((name) => name !== labelName);
  selectedLabels.value = next;
  savingLabels.value = true;
  try {
    await issueApiClient.issue.patchIssue({
      name: props.issue.issue.metadata.name,
      jsonPatchInner: [{ op: "add", path: "/spec/labels", value: next }],
    });
    Toast.success("标签已更新");
    await queryClient.invalidateQueries({ queryKey: ["issues"] });
  } catch (error) {
    selectedLabels.value = previous;
    console.error(error);
    Toast.error("标签更新失败");
  } finally {
    savingLabels.value = false;
  }
}

function deleteIssue() {
  Dialog.warning({
    title: "删除 Issue",
    description: "该操作会同时删除此 Issue 下的全部评论，且无法恢复。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      await consoleIssueApiClient.issue.deleteIssue({
        name: props.issue.issue.metadata.name,
      });
      Toast.success("删除成功");
      await queryClient.invalidateQueries({ queryKey: ["issues"] });
    },
  });
}

async function approveIssue() {
  await issueApiClient.issue.patchIssue({
    name: props.issue.issue.metadata.name,
    jsonPatchInner: [{ op: "add", path: "/spec/approved", value: true }],
  });
  Toast.success("审核成功");
  await queryClient.invalidateQueries({ queryKey: ["issues"] });
}

async function submitClose() {
  closing.value = true;
  try {
    await consoleIssueApiClient.issue.updateIssueStatus({
      issueStatusChangeParam: {
        issueName: props.issue.issue.metadata.name,
        issueState: "CLOSED",
        changeComment: closeComment.value,
      },
    });
    Toast.success("关闭 Issue 成功");
    closeVisible.value = false;
    closeComment.value = "";
    await queryClient.invalidateQueries({ queryKey: ["issues"] });
  } finally {
    closing.value = false;
  }
}

async function reopenIssue() {
  await consoleIssueApiClient.issue.updateIssueStatus({
    issueStatusChangeParam: {
      issueName: props.issue.issue.metadata.name,
      issueState: "PROGRESS",
      changeComment: "重新打开 Issue",
    },
  });
  Toast.success("操作成功");
  await queryClient.invalidateQueries({ queryKey: ["issues"] });
}
</script>

<template>
  <VModal
    :title="issue.issue.spec.title"
    :visible="detailVisible"
    :width="900"
    @update:visible="detailVisible = $event"
  >
    <div class="space-y-5">
      <div class="flex flex-wrap items-center gap-3 text-xs text-gray-500">
        <VStatusDot :state="statusState" :text="statusText" />
        <span>创建者：{{ issue.contributorVo.displayName }}</span>
        <span v-if="issue.issue.metadata.creationTimestamp">
          创建于 {{ formatDatetime(issue.issue.metadata.creationTimestamp) }}
        </span>
      </div>
      <div v-if="selectedLabels.length" class="flex flex-wrap gap-2">
        <span
          v-for="name in selectedLabels"
          :key="name"
          class="rounded-full px-2.5 py-1 text-xs text-white"
          :style="{ backgroundColor: labelOption(name)?.color || '#6B7280' }"
        >
          {{ labelOption(name)?.label || name }}
        </span>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-5">
        <div
          v-if="issue.issue.spec.content.html"
          class="issue-content break-words text-sm leading-7 text-gray-800"
          v-html="issue.issue.spec.content.html"
        />
        <p
          v-else-if="issue.issue.spec.content.raw"
          class="whitespace-pre-wrap break-words text-sm leading-7"
        >
          {{ issue.issue.spec.content.raw }}
        </p>
        <VEmpty v-else title="此 Issue 暂无正文" />
      </div>
      <div>
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-sm font-medium">
            评论（{{ issue.issueStats.totalIssueComment || 0 }}）
          </h3>
          <VButton size="sm" @click="refetchComments()">刷新评论</VButton>
        </div>
        <VLoading v-if="isLoading" />
        <VEmpty v-else-if="!issueComments?.length" title="暂无评论" />
        <VEntityContainer v-else>
          <IssueCommentItem
            v-for="comment in issueComments"
            :key="comment.issueComment.metadata.name"
            :comment="comment"
            :comments="issueComments"
            @update-issue-comments="refetchComments()"
          />
        </VEntityContainer>
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton type="secondary" @click="emit('update', issue.issue)">编辑</VButton>
        <VButton @click="detailVisible = false">关闭</VButton>
      </VSpace>
    </template>
  </VModal>

  <VModal title="关闭 Issue" :visible="closeVisible" :width="420">
    <VAlert type="info" title="提示" description="关闭后仍可重新打开。" />
    <FormKit v-model.trim="closeComment" type="text" label="关闭原因" />
    <template #footer>
      <VSpace>
        <VButton :loading="closing" type="secondary" @click="submitClose()">提交</VButton>
        <VButton @click="closeVisible = false">取消</VButton>
      </VSpace>
    </template>
  </VModal>

  <VEntity :is-selected="isSelected">
    <template #checkbox>
      <HasPermission :permissions="['plugin:issues:manage']">
        <input
          v-model="selectedIssueNames"
          :value="issue.issue.metadata.name"
          type="checkbox"
        />
      </HasPermission>
    </template>
    <template #start>
      <VEntityField :title="issue.issue.spec.title" width="30rem">
        <template #description>
          <div class="mt-1 flex flex-col gap-2">
            <div class="flex flex-wrap items-center gap-2">
              <button
                type="button"
                class="text-xs font-medium text-blue-600 hover:text-blue-700"
                @click="detailVisible = true"
              >
                查看内容
              </button>
              <span class="text-xs text-gray-500">
                评论 {{ issue.issueStats.totalIssueComment || 0 }}
              </span>
              <span class="text-xs text-gray-500">
                点赞 {{ issue.issueStats.upvote || 0 }}
              </span>
            </div>
            <div class="flex flex-wrap items-center gap-1.5">
              <span
                v-for="name in selectedLabels"
                :key="name"
                class="rounded-full px-2 py-0.5 text-xs text-white"
                :style="{ backgroundColor: labelOption(name)?.color || '#6B7280' }"
              >
                {{ labelOption(name)?.label || name }}
              </span>
              <HasPermission :permissions="['plugin:issues:manage']">
                <details class="relative">
                  <summary
                    class="cursor-pointer list-none rounded border border-dashed border-gray-300 px-2 py-0.5 text-xs"
                  >
                    {{ savingLabels ? "保存中…" : "+ 标签" }}
                  </summary>
                  <div
                    class="absolute left-0 z-30 mt-1 w-60 rounded-lg border bg-white p-2 shadow-lg"
                  >
                    <p class="px-2 pb-2 text-xs font-medium">为此 Issue 分配标签</p>
                    <p v-if="!labelOptions.length" class="px-2 py-3 text-xs text-gray-500">
                      暂无全局标签，请先在 Issue 标签中创建。
                    </p>
                    <label
                      v-for="option in labelOptions"
                      :key="option.value"
                      class="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 hover:bg-gray-50"
                    >
                      <input
                        v-if="option.value"
                        type="checkbox"
                        :checked="selectedLabels.includes(option.value)"
                        :disabled="savingLabels"
                        @change="
                          changeLabel(
                            option.value,
                            ($event.target as HTMLInputElement).checked,
                          )
                        "
                      />
                      <span
                        class="h-2.5 w-2.5 rounded-full"
                        :style="{ backgroundColor: option.color || '#6B7280' }"
                      />
                      <span class="truncate text-xs">{{ option.label }}</span>
                    </label>
                  </div>
                </details>
              </HasPermission>
            </div>
          </div>
        </template>
      </VEntityField>
    </template>
    <template #end>
      <VEntityField>
        <template #description>
          <VStatusDot :state="statusState" :text="statusText" />
        </template>
      </VEntityField>
      <VEntityField>
        <template #description>
          <VAvatar
            v-tooltip="issue.contributorVo.displayName"
            :src="issue.contributorVo.avatar"
            :alt="issue.contributorVo.displayName"
            size="xs"
            circle
          />
        </template>
      </VEntityField>
      <VEntityField v-if="issue.issue.metadata.creationTimestamp">
        <template #description>
          <span class="truncate text-xs text-gray-500">
            {{ formatDatetime(issue.issue.metadata.creationTimestamp) }}
          </span>
        </template>
      </VEntityField>
    </template>
    <template #dropdownItems>
      <VDropdownItem v-if="!issue.issue.spec.approved" @click="approveIssue()">
        审核
      </VDropdownItem>
      <VDropdownItem @click="emit('update', issue.issue)">编辑</VDropdownItem>
      <HasPermission :permissions="['plugin:issues:manage']">
        <VDropdownItem
          v-if="issue.issue.status?.state !== 'CLOSED'"
          @click="closeVisible = true"
        >
          关闭
        </VDropdownItem>
        <VDropdownItem v-else @click="reopenIssue()">重新打开</VDropdownItem>
        <VDropdownDivider />
        <VDropdownItem type="danger" @click="deleteIssue()">删除</VDropdownItem>
      </HasPermission>
    </template>
  </VEntity>
</template>

<style scoped>
.issue-content :deep(img) {
  max-width: 100%;
  height: auto;
}
.issue-content :deep(pre) {
  overflow-x: auto;
}
</style>

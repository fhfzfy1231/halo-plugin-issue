import { definePlugin } from "@halo-dev/ui-shared";
import PajamasIssueTypeObjective from "~icons/pajamas/issue-type-objective";
import FluentMailTemplate20Regular from "~icons/fluent/mail-template-20-regular";
import PepiconsPrintLabelCircle from "~icons/pepicons-print/label-circle";
import { markRaw } from "vue";
import IssueList from "@/views/IssueList.vue";
import IssueTemplateList from "@/views/IssueTemplateList.vue";
import IssueTemplateEditor from "@/views/IssueTemplateEditor.vue";
import IssueLabelList from "@/views/IssueLabelList.vue";
import "uno.css";
import "./styles/index.scss";

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/issues",
        name: "Issue",
        component: IssueList,
        meta: {
          title: "Issue 列表",
          searchable: true,
          mobile: true,
          permissions: ["plugin:issues:manage"],
          menu: {
            name: "Issue 列表",
            group: "content",
            icon: markRaw(PajamasIssueTypeObjective),
            mobile: true,
          },
        },
      },
    },
    {
      parentName: "Root",
      route: {
        path: "/issue-templates",
        name: "IssueTemplate",
        component: IssueTemplateList,
        meta: {
          title: "Issue 模板",
          searchable: true,
          permissions: ["plugin:issueTemplates:manage"],
          menu: {
            name: "Issue 模板",
            group: "content",
            icon: markRaw(FluentMailTemplate20Regular),
          },
        },
      },
    },
    {
      parentName: "Root",
      route: {
        path: "/issue-templates/editor",
        name: "IssueTemplateEditor",
        component: IssueTemplateEditor,
        meta: {
          title: "编辑 Issue 模板",
          permissions: ["plugin:issueTemplates:manage"],
        },
      },
    },
    {
      parentName: "Root",
      route: {
        path: "/issue-labels",
        name: "IssueLabel",
        component: IssueLabelList,
        meta: {
          title: "Issue 标签",
          searchable: true,
          permissions: ["plugin:issue:labels:manage"],
          menu: {
            name: "Issue 标签",
            group: "content",
            icon: markRaw(PepiconsPrintLabelCircle),
          },
        },
      },
    },
  ],
  extensionPoints: {},
});

import { definePlugin } from "@halo-dev/ui-shared";
import PajamasIssueTypeObjective from "~icons/pajamas/issue-type-objective";
import FluentMailTemplate20Regular from "~icons/fluent/mail-template-20-regular";
import PepiconsPrintLabelCircle from "~icons/pepicons-print/label-circle";
import { markRaw } from "vue";
import { RouterView } from "vue-router";
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
        name: "IssueRoot",
        component: RouterView,
        meta: {
          title: "Issue",
          mobile: true,
          permissions: [
            "plugin:issues:manage",
            "plugin:issueTemplates:manage",
            "plugin:issue:labels:manage",
          ],
          menu: {
            name: "Issue",
            group: "content",
            icon: markRaw(PajamasIssueTypeObjective),
            mobile: true,
          },
        },
        children: [
          {
            path: "",
            redirect: { name: "Issue" },
          },
          {
            path: "list",
            name: "Issue",
            component: IssueList,
            meta: {
              title: "Issue 列表",
              searchable: true,
              mobile: true,
              permissions: ["plugin:issues:manage"],
              menu: {
                name: "Issue 列表",
                icon: markRaw(PajamasIssueTypeObjective),
                mobile: true,
                priority: 0,
              },
            },
          },
          {
            path: "templates",
            name: "IssueTemplate",
            component: IssueTemplateList,
            meta: {
              title: "Issue 模板",
              searchable: true,
              permissions: ["plugin:issueTemplates:manage"],
              menu: {
                name: "Issue 模板",
                icon: markRaw(FluentMailTemplate20Regular),
                priority: 10,
              },
            },
          },
          {
            path: "templates/editor",
            name: "IssueTemplateEditor",
            component: IssueTemplateEditor,
            meta: {
              title: "编辑 Issue 模板",
              permissions: ["plugin:issueTemplates:manage"],
            },
          },
          {
            path: "labels",
            name: "IssueLabel",
            component: IssueLabelList,
            meta: {
              title: "Issue 标签",
              searchable: true,
              permissions: ["plugin:issue:labels:manage"],
              menu: {
                name: "Issue 标签",
                icon: markRaw(PepiconsPrintLabelCircle),
                priority: 20,
              },
            },
          },
        ],
      },
    },
    {
      parentName: "Root",
      route: {
        path: "/issue-templates",
        redirect: { name: "IssueTemplate" },
      },
    },
    {
      parentName: "Root",
      route: {
        path: "/issue-labels",
        redirect: { name: "IssueLabel" },
      },
    },
  ],
  extensionPoints: {},
});

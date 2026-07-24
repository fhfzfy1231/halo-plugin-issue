import { updateIssueLabels } from "../api/issue";

interface State {
    saving: boolean;
    saveText: string;
    save: () => Promise<void>;
}

export default (): State => ({
    saving: false,
    saveText: "保存标签",

    async save() {
        if (this.saving) {
            return;
        }

        const root = (this as State & { $root: HTMLElement }).$root;
        const issueName = root.dataset.issueName;
        if (!issueName) {
            return;
        }

        const labels = Array.from(
            root.querySelectorAll<HTMLInputElement>("input[data-issue-label]:checked"),
        ).map((input) => input.value);

        this.saving = true;
        this.saveText = "保存中…";
        try {
            await updateIssueLabels(issueName, labels);
            window.location.reload();
        } catch (error) {
            console.error(error);
            this.saveText = "保存失败";
            window.setTimeout(() => {
                this.saveText = "保存标签";
            }, 1800);
        } finally {
            this.saving = false;
        }
    },
});

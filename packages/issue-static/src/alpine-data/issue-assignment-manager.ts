import {
    searchAssignableUsers,
    updateIssueAssignees,
    updateIssueLabels,
} from "../api/issue";

interface AssignableUser {
    name: string;
    displayName: string;
    avatar?: string;
}

interface State {
    assigneeOpen: boolean;
    labelOpen: boolean;
    savingAssignees: boolean;
    savingLabels: boolean;
    loadingUsers: boolean;
    userKeyword: string;
    users: AssignableUser[];
    selectedAssignees: string[];
    selectedLabels: string[];
    searchTimer?: number;
    init: () => void;
    toggleAssignee: () => void;
    toggleLabel: () => void;
    searchUsers: () => void;
    loadUsers: () => Promise<void>;
    toggleAssigneeValue: (name: string) => void;
    toggleLabelValue: (name: string) => void;
    saveAssignees: () => Promise<void>;
    saveLabels: () => Promise<void>;
}

type AlpineState = State & { $root: HTMLElement };

function initialValues(root: HTMLElement, key: "assignees" | "labels"): string[] {
    const value = root.dataset[key];
    if (!value) {
        return [];
    }
    try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed : [];
    } catch {
        return value.split(",").filter(Boolean);
    }
}

export default (): State => ({
    assigneeOpen: false,
    labelOpen: false,
    savingAssignees: false,
    savingLabels: false,
    loadingUsers: false,
    userKeyword: "",
    users: [],
    selectedAssignees: [],
    selectedLabels: [],

    init() {
        const root = (this as AlpineState).$root;
        this.selectedAssignees = initialValues(root, "assignees");
        this.selectedLabels = initialValues(root, "labels");
    },

    toggleAssignee() {
        this.assigneeOpen = !this.assigneeOpen;
        this.labelOpen = false;
        if (this.assigneeOpen && this.users.length === 0) {
            void this.loadUsers();
        }
    },

    toggleLabel() {
        this.labelOpen = !this.labelOpen;
        this.assigneeOpen = false;
    },

    searchUsers() {
        window.clearTimeout(this.searchTimer);
        this.searchTimer = window.setTimeout(() => {
            void this.loadUsers();
        }, 250);
    },

    async loadUsers() {
        this.loadingUsers = true;
        try {
            const response = await searchAssignableUsers(this.userKeyword.trim());
            this.users = response.data as AssignableUser[];
        } catch (error) {
            console.error(error);
            this.users = [];
        } finally {
            this.loadingUsers = false;
        }
    },

    toggleAssigneeValue(name: string) {
        this.selectedAssignees = this.selectedAssignees.includes(name)
            ? this.selectedAssignees.filter((item) => item !== name)
            : [...this.selectedAssignees, name];
    },

    toggleLabelValue(name: string) {
        this.selectedLabels = this.selectedLabels.includes(name)
            ? this.selectedLabels.filter((item) => item !== name)
            : [...this.selectedLabels, name];
    },

    async saveAssignees() {
        if (this.savingAssignees) {
            return;
        }
        const issueName = (this as AlpineState).$root.dataset.issueName;
        if (!issueName) {
            return;
        }
        this.savingAssignees = true;
        try {
            await updateIssueAssignees(issueName, this.selectedAssignees);
            window.location.reload();
        } catch (error) {
            console.error(error);
        } finally {
            this.savingAssignees = false;
        }
    },

    async saveLabels() {
        if (this.savingLabels) {
            return;
        }
        const issueName = (this as AlpineState).$root.dataset.issueName;
        if (!issueName) {
            return;
        }
        this.savingLabels = true;
        try {
            await updateIssueLabels(issueName, this.selectedLabels);
            window.location.reload();
        } catch (error) {
            console.error(error);
        } finally {
            this.savingLabels = false;
        }
    },
});

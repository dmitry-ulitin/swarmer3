import { Category } from "./category";

export enum ConditionType {
    PARTY_EQUALS = 1,
    PARTY_CONTAINS,
    DETAILS_EQUALS,
    DETAILS_CONTAINS,
    CATNAME_EQUALS,
    CATNAME_CONTAINS
}

export interface Rule {
    id?: number;
    conditionType: ConditionType;
    conditionValue: string;
    category: Category;
}
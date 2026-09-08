export type QuizType = 'CONTROL' | 'FINAL_EXAM';
export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE';

export interface OptionView {
  id: number;
  label: string;
  position: number;
  correct: boolean | null;
}

export interface QuestionView {
  id: number;
  statement: string;
  type: QuestionType;
  points: number;
  position: number;
  options: OptionView[];
}

export interface QuizView {
  id: number;
  type: QuizType;
  title: string;
  passThreshold: number;
  maxAttempts: number | null;
  answersVisible: boolean;
  questions: QuestionView[];
}

export interface QuizRef {
  quizId: number;
  chapterId: number | null;
  type: QuizType;
  title: string;
  questionCount: number;
  maxAttempts: number | null;
}

export interface CourseQuizzes {
  controls: QuizRef[];
  finalExam: QuizRef | null;
}

export interface AttemptResult {
  attemptId: number;
  score: number;
  passed: boolean;
  correctCount: number;
  total: number;
  controlsAverage: number | null;
  finalGrade: number | null;
  certificateId: number | null;
}

export interface AttemptSummary {
  id: number;
  score: number;
  passed: boolean;
  submittedAt: string;
}

export interface ControlScore {
  quizId: number;
  chapterId: number | null;
  bestScore: number;
  attempts: number;
}

export interface CourseGrade {
  courseId: number;
  progressPercent: number;
  finalExamUnlocked: boolean;
  controlsAverage: number | null;
  controls: ControlScore[];
  finalExamBestScore: number | null;
  finalGrade: number | null;
  passThreshold: number;
  certificateId: number | null;
}

export interface LearnerResult {
  userId: number;
  learnerName: string;
  controlsAverage: number | null;
  finalExamBestScore: number | null;
  finalGrade: number | null;
  certified: boolean;
}

export interface OptionInput {
  label: string;
  correct: boolean;
  position: number;
}

export interface QuestionInput {
  statement: string;
  type: QuestionType;
  points: number;
  position: number;
  options: OptionInput[];
}

export interface AnswerInput {
  questionId: number;
  selectedOptionIds: number[];
}

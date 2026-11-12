"""Regresiones para el alcance de los hooks y del workflow."""

import importlib.util
import io
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "quality", Path(__file__).with_name("quality.py")
)
quality = importlib.util.module_from_spec(spec)
spec.loader.exec_module(quality)


class Gates(unittest.TestCase):
    """Comprueba cuándo los hooks y CI deben ejecutar la validación Python."""

    def calls(self, stage, branch, refs=""):
        """Simula etapa, rama y referencias y devuelve cuántas veces se solicita la suite."""
    def calls(self, stage, branch, refs=""):
        with (
            patch.object(sys, "argv", ["quality", stage, "--branch", branch]),
            patch.object(sys, "stdin", io.StringIO(refs)),
            patch.object(quality, "suite") as suite,
        ):
            quality.main()
            return suite.call_count

    def test_exact_python_branches(self):
        """Activa la validación para python-cli y ramas cuyo segmento final es python."""
        for branch in ("python", "python-cli", "feature/work/python"):
            with self.subTest(branch=branch):
                self.assertTrue(quality.is_python_branch(branch))
                self.assertEqual(self.calls("pre-commit", branch), 1)

    def test_other_branches_are_unaffected(self):
        """Evita ejecutar las revisiones Python en ramas de otras plataformas."""
        for branch in (
            "main",
            "develop",
            "ci/cd",
            "feature/javafx",
            "feature/android",
            "feature/Unity",
            "nopython",
            "python-legacy",
            "feature/PYTHON",
            "",
        ):
            with self.subTest(branch=branch):
                self.assertFalse(quality.is_python_branch(branch))
                self.assertEqual(self.calls("pre-commit", branch), 0)

    def test_push_uses_sent_refs(self):
        """Decide la validación por las referencias enviadas y no por el checkout actual."""
        for checkout in ("python-cli", "feature/javafx"):
            with self.subTest(checkout=checkout):
                self.assertEqual(
                    self.calls(
                        "pre-push",
                        checkout,
                        "refs/heads/feature/javafx abc refs/heads/feature/javafx def\n",
                    ),
                    0,
                )
                self.assertEqual(
                    self.calls(
                        "pre-push",
                        checkout,
                        "refs/heads/python-cli abc refs/heads/python-cli def\n",
                    ),
                    1,
                )

    def test_deletions_and_tags_do_not_run_python(self):
        """Omite la validación al enviar etiquetas, borrados o ninguna referencia."""
        for refs in (
            "",
            "refs/tags/v1 abc refs/tags/v1 def\n",
            "refs/tags/python abc refs/tags/python def\n",
            "(delete) 000000 refs/heads/python-cli abc\n",
        ):
            self.assertEqual(self.calls("pre-push", "python-cli", refs), 0)

    def test_explicit_checks_need_no_git(self):
        """Ejecuta ci y test sin consultar la rama Git."""
        for stage in ("ci", "test"):
            with (
                patch.object(sys, "argv", ["quality", stage]),
                patch.object(
                    quality.subprocess, "check_output", side_effect=AssertionError
                ),
                patch.object(quality, "suite") as suite,
            ):
                quality.main()
                suite.assert_called_once_with()

    def test_missing_application_fails_explicit_validation(self):
        """Comunica la ausencia del código antes de intentar ejecutar herramientas."""
        with patch.object(quality, "ROOT", Path("/missing-enigma-project")):
            with self.assertRaisesRegex(ValueError, "integra el código Python"):
                quality.suite()


if __name__ == "__main__":
    unittest.main()

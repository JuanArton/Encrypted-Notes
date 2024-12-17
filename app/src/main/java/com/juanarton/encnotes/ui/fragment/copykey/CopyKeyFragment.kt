package com.juanarton.encnotes.ui.fragment.copykey

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.juanarton.encnotes.R
import com.juanarton.encnotes.databinding.FragmentCopyKeyBinding
import com.juanarton.encnotes.ui.activity.main.MainActivity
import com.juanarton.encnotes.ui.fragment.SharedViewModel
import com.juanarton.encnotes.ui.fragment.loading.LoadingFragment
import com.juanarton.encnotes.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CopyKeyFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private var _binding: FragmentCopyKeyBinding? = null
    private val binding get() = _binding
    private val loadingDialog = LoadingFragment()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCopyKeyBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val key = arguments?.getString("KEY_STRING")

        binding?.apply {
            tvKey.text = key

            btDone.setOnClickListener {
                lifecycleScope.launch {
                    val setCipherKey = sharedViewModel.setCipherKey(key!!)

                    if (setCipherKey) {
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.login_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        FragmentBuilder.destroyFragment(requireActivity(), loadingDialog)
                    }
                }
            }

            ibCopy.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", tvKey.text)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
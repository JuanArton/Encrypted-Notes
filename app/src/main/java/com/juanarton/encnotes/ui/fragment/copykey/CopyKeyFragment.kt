package com.juanarton.encnotes.ui.fragment.copykey

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.FragmentCopyKeyBinding
import com.juanarton.encnotes.ui.LoadingDialog
import com.juanarton.encnotes.ui.activity.main.MainActivity
import com.juanarton.encnotes.ui.fragment.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CopyKeyFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private var _binding: FragmentCopyKeyBinding? = null
    private val binding get() = _binding
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCopyKeyBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = LoadingDialog(requireContext())

        val uid = arguments?.getString("uid")
        val pin = arguments?.getString("pin")

        if (!uid.isNullOrEmpty() && !pin.isNullOrEmpty()) {
            val key = Cryptography.serializeKeySet(Cryptography.generateKeySet())

            binding?.apply {
                tvKey.text = key

                btDone.setOnClickListener {
                    sharedViewModel.loginUser(uid, pin)
                }

                ibCopy.setOnClickListener {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied Text", tvKey.text)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }

            sharedViewModel.loginUser.observe(viewLifecycleOwner) { result ->
                when(result){
                    is Resource.Success -> {
                        result.data?.let { login ->
                            lifecycleScope.launch {
                                val setAccKey = sharedViewModel.setAccessKey(login.accessToken)
                                val setRefKey = sharedViewModel.setRefreshKey(login.refreshToken)
                                val setLoggedIn = sharedViewModel.setIsLoggedIn(true)
                                val setCipherKey = sharedViewModel.setCipherKey(key)

                                if (setAccKey && setRefKey && setLoggedIn && setCipherKey) {
                                    loadingDialog.dismiss()
                                    startActivity(Intent(requireContext(), MainActivity::class.java))
                                    requireActivity().finish()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    loadingDialog.dismiss()
                                }
                            }
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("Copy Key Fragment", "Loading")
                        loadingDialog.show()
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            result.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingDialog.dismiss()
                    }
                }
            }
        }
    }
}